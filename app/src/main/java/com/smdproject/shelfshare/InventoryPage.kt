package com.smdproject.shelfshare

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.TranslateAnimation
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.ArgbEvaluator
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class InventoryPage : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var rootLayout: RelativeLayout
    private lateinit var headerLayout: RelativeLayout
    private lateinit var logoImageView: ImageView
    private lateinit var menuIcon: ImageView
    private lateinit var searchIcon: ImageView
    private lateinit var addBookButton: RelativeLayout
    private lateinit var bookAdapter: InventoryBookAdapter
    private val bookList = mutableListOf<InventoryBook>() // Ensure this is the list used everywhere

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_inventory_page)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        val userId = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "User not authenticated. Please log in.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LogInPage::class.java))
            finish()
            return
        }

        rootLayout = findViewById(R.id.main)
        headerLayout = findViewById(R.id.header)
        logoImageView = findViewById(R.id.logoImageView)
        menuIcon = findViewById(R.id.menu_icon)
        searchIcon = findViewById(R.id.search_icon)
        addBookButton = findViewById(R.id.addBook)

        val recyclerView = findViewById<RecyclerView>(R.id.bookRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        bookAdapter = InventoryBookAdapter(bookList) { loadInventory(userId) }
        recyclerView.adapter = bookAdapter // Ensure adapter is attached

        headerLayout.setBackgroundColor(android.graphics.Color.WHITE)
        logoImageView.setImageResource(R.drawable.logo_orange_right)
        menuIcon.setImageResource(R.drawable.menu_logo)
        searchIcon.setImageResource(R.drawable.search_logo)

        val slideInFromRight = TranslateAnimation(1000f, 0f, 0f, 0f)
        slideInFromRight.duration = 1000
        val fadeIn = AlphaAnimation(0f, 1f)
        fadeIn.duration = 1000
        val animationSet = AnimationSet(true)
        animationSet.addAnimation(slideInFromRight)
        animationSet.addAnimation(fadeIn)
        rootLayout.startAnimation(animationSet)

        animationSet.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
            override fun onAnimationStart(animation: android.view.animation.Animation?) {}
            override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                val colorFrom = android.graphics.Color.WHITE
                val colorTo = resources.getColor(R.color.AppPrimary, theme)
                val colorAnimation = ValueAnimator.ofObject(android.animation.ArgbEvaluator(), colorFrom, colorTo)
                colorAnimation.duration = 1000
                colorAnimation.addUpdateListener { animator ->
                    headerLayout.setBackgroundColor(animator.animatedValue as Int)
                }
                colorAnimation.start()
                logoImageView.setImageResource(R.drawable.logo_white_right)
                menuIcon.setImageResource(R.drawable.menu_logo)
                searchIcon.setImageResource(R.drawable.search_logo)
            }
            override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
        })

        loadInventory(userId)

        addBookButton.setOnClickListener { showAddBookDialog(userId) }

        fun applyExitAnimationAndNavigate(targetActivity: Class<*>, toastMessage: String) {
            val fadeOut = AlphaAnimation(1f, 0f)
            fadeOut.duration = 2000
            val slideOutToRight = TranslateAnimation(0f, 1000f, 0f, 0f)
            slideOutToRight.duration = 2000
            val outAnimationSet = AnimationSet(true)
            outAnimationSet.addAnimation(fadeOut)
            outAnimationSet.addAnimation(slideOutToRight)
            outAnimationSet.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                override fun onAnimationStart(animation: android.view.animation.Animation?) { rootLayout.isEnabled = false }
                override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
                override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                    rootLayout.visibility = View.GONE
                    startActivity(Intent(this@InventoryPage, targetActivity))
                    finish()
                }
            })
            rootLayout.startAnimation(outAnimationSet)
            Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show()
        }

        menuIcon.setOnClickListener { applyExitAnimationAndNavigate(MenuPage::class.java, "Navigating to MenuPage") }
        searchIcon.setOnClickListener { applyExitAnimationAndNavigate(SearchPage::class.java, "Navigating to SearchPage") }
    }

    private fun loadInventory(userId: String) {
        bookList.clear() // Ensure list is cleared before loading
        Log.d("InventoryPage", "Fetching inventory for user: $userId")
        database.child("Inventory").child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    Log.d("InventoryPage", "No books found in inventory for user: $userId")
                    bookAdapter.submitBooks(bookList)
                    return
                }
                Log.d("InventoryPage", "Found ${snapshot.childrenCount} book IDs in inventory")
                val totalBooks = snapshot.childrenCount.toInt()
                var booksProcessed = 0

                snapshot.children.forEach { bookSnapshot ->
                    val bookId = bookSnapshot.key ?: return@forEach
                    Log.d("InventoryPage", "Processing bookId: $bookId")
                    database.child("book").child(bookId).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(bookSnapshot: DataSnapshot) {
                            if (bookSnapshot.exists()) {
                                val image = bookSnapshot.child("image").getValue(String::class.java)
                                val name = bookSnapshot.child("name").getValue(String::class.java)
                                val author = bookSnapshot.child("author").getValue(String::class.java)
                                val description = bookSnapshot.child("description").getValue(String::class.java)
                                val categories = bookSnapshot.child("categories").children.mapNotNull { it.getValue(String::class.java) }
                                val book = InventoryBook(bookId, image, name, author, description, categories)
                                bookList.add(book) // Add to the same list instance
                                Log.d("InventoryPage", "Added book to list: $name")
                            } else {
                                Log.w("InventoryPage", "Book not found in /book for bookId: $bookId")
                            }
                            booksProcessed++
                            if (booksProcessed == totalBooks) {
                                Log.d("InventoryPage", "All books processed, updating adapter with ${bookList.size} books")
                                bookAdapter.submitBooks(bookList.toList()) // Use toList() to pass a copy
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {
                            Log.e("InventoryPage", "Failed to load book $bookId: ${error.message}")
                            booksProcessed++
                            if (booksProcessed == totalBooks) {
                                Log.d("InventoryPage", "All books processed (with errors), updating adapter with ${bookList.size} books")
                                bookAdapter.submitBooks(bookList.toList())
                            }
                        }
                    })
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("InventoryPage", "Failed to load inventory: ${error.message}")
                if (error.code == DatabaseError.PERMISSION_DENIED) {
                    startActivity(Intent(this@InventoryPage, LogInPage::class.java))
                    finish()
                }
            }
        })
    }

    private fun showAddBookDialog(userId: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_book, null)
        val bookNameInput = dialogView.findViewById<EditText>(R.id.bookNameInput)
        val bookAuthorInput = dialogView.findViewById<EditText>(R.id.bookAuthorInput)
        val bookDescriptionInput = dialogView.findViewById<EditText>(R.id.bookDescriptionInput)
        val bookCategoriesInput = dialogView.findViewById<EditText>(R.id.bookCategoriesInput)
        val bookImageQueryInput = dialogView.findViewById<EditText>(R.id.bookImageQueryInput)
        val saveButton = dialogView.findViewById<Button>(R.id.saveButton)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Add New Book")
            .setView(dialogView)
            .create()

        saveButton.setOnClickListener {
            val name = bookNameInput.text.toString().trim()
            val author = bookAuthorInput.text.toString().trim()
            val description = bookDescriptionInput.text.toString().trim()
            val categoriesInput = bookCategoriesInput.text.toString().trim()
            val imageQuery = bookImageQueryInput.text.toString().trim()

            if (name.isEmpty()) {
                bookNameInput.error = "Book name is required"
                return@setOnClickListener
            }

            val categories = categoriesInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val bookId = database.child("book").push().key ?: return@setOnClickListener
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener

            val bookData = mapOf(
                "image" to (if (imageQuery.isNotEmpty()) imageQuery else null),
                "name" to name,
                "author" to (if (author.isNotEmpty()) author else null),
                "description" to (if (description.isNotEmpty()) description else null),
                "categories" to categories,
                "ownerId" to userId // Add ownerId to the book node
            )

            database.child("book").child(bookId).setValue(bookData)
                .addOnSuccessListener {
                    database.child("Inventory").child(userId).child(bookId).setValue(true)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Book added successfully", Toast.LENGTH_SHORT).show()
                            loadInventory(userId)
                            dialog.dismiss()
                        }
                        .addOnFailureListener { error ->
                            Log.e("InventoryPage", "Failed to add book to inventory: ${error.message}")
                            Toast.makeText(this, "Failed to add book to inventory: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { error ->
                    Log.e("InventoryPage", "Failed to add book: ${error.message}")
                    Toast.makeText(this, "Failed to add book: ${error.message}", Toast.LENGTH_SHORT).show()
                }
        }

        cancelButton.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }
}