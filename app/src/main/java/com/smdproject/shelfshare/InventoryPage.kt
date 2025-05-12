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
    private val bookList = mutableListOf<InventoryBook>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_inventory_page)

        // Handle window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        val userId = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "User not authenticated. Please log in.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LogInPage::class.java))
            finish()
            return
        }

        // Initialize views
        rootLayout = findViewById(R.id.main)
        headerLayout = findViewById(R.id.header)
        logoImageView = findViewById(R.id.logoImageView)
        menuIcon = findViewById(R.id.menu_icon)
        searchIcon = findViewById(R.id.search_icon)
        addBookButton = findViewById(R.id.addBook)

        // Set up RecyclerView for books
        val recyclerView = findViewById<RecyclerView>(R.id.bookRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        bookAdapter = InventoryBookAdapter(bookList) { loadInventory(userId) }
        recyclerView.adapter = bookAdapter

        // Set initial state for animation (white header and orange logo)
        headerLayout.setBackgroundColor(android.graphics.Color.WHITE)
        logoImageView.setImageResource(R.drawable.logo_orange_right)
        menuIcon.setImageResource(R.drawable.menu_logo)
        searchIcon.setImageResource(R.drawable.search_logo)

        // Create a TranslateAnimation to slide in from the right
        val slideInFromRight = TranslateAnimation(
            1000f,  // Start from 1000px to the right
            0f,     // End at its normal position
            0f,     // No vertical movement
            0f
        )
        slideInFromRight.duration = 1000 // 1 second

        // Create an AlphaAnimation for fading in
        val fadeIn = AlphaAnimation(0f, 1f)
        fadeIn.duration = 1000 // 1 second

        // Combine both animations into an AnimationSet
        val animationSet = AnimationSet(true)
        animationSet.addAnimation(slideInFromRight)
        animationSet.addAnimation(fadeIn)

        // Apply the animation to the root layout
        rootLayout.startAnimation(animationSet)

        // Change header background to AppPrimary and logo to white_right after animation ends
        animationSet.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
            override fun onAnimationStart(animation: android.view.animation.Animation?) {}

            override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                // Fade header background from white to AppPrimary
                val colorFrom = android.graphics.Color.WHITE
                val colorTo = resources.getColor(R.color.AppPrimary, theme)
                val colorAnimation = ValueAnimator.ofObject(android.animation.ArgbEvaluator(), colorFrom, colorTo)
                colorAnimation.duration = 1000 // 1 second
                colorAnimation.addUpdateListener { animator ->
                    headerLayout.setBackgroundColor(animator.animatedValue as Int)
                }
                colorAnimation.start()

                // Change logo and icons to white variants after animation
                logoImageView.setImageResource(R.drawable.logo_white_right)
                menuIcon.setImageResource(R.drawable.menu_logo)
                searchIcon.setImageResource(R.drawable.search_logo)
            }

            override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
        })

        // Load user's inventory
        loadInventory(userId)

        // Set click listener for Add Book button
        addBookButton.setOnClickListener {
            showAddBookDialog(userId)
        }

        // Helper function to apply exit animation and navigate
        fun applyExitAnimationAndNavigate(targetActivity: Class<*>, toastMessage: String) {
            val fadeOut = AlphaAnimation(1f, 0f)
            fadeOut.duration = 2000
            val slideOutToRight = TranslateAnimation(0f, 1000f, 0f, 0f)
            slideOutToRight.duration = 2000
            val outAnimationSet = AnimationSet(true)
            outAnimationSet.addAnimation(fadeOut)
            outAnimationSet.addAnimation(slideOutToRight)

            outAnimationSet.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                override fun onAnimationStart(animation: android.view.animation.Animation?) {
                    rootLayout.isEnabled = false
                }

                override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}

                override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                    rootLayout.visibility = View.GONE
                    val intent = Intent(this@InventoryPage, targetActivity)
                    startActivity(intent)
                    finish()
                }
            })

            rootLayout.startAnimation(outAnimationSet)
            Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show()
        }

        // Set click listeners for navigation
        menuIcon.setOnClickListener {
            applyExitAnimationAndNavigate(MenuPage::class.java, "Navigating to MenuPage")
        }

        searchIcon.setOnClickListener {
            applyExitAnimationAndNavigate(SearchPage::class.java, "Navigating to SearchPage")
        }
    }

    private fun loadInventory(userId: String) {
        bookList.clear()
        Log.d("InventoryPage", "Fetching inventory for user: $userId")
        Toast.makeText(this, "Fetching inventory for user: $userId", Toast.LENGTH_SHORT).show()
        database.child("Inventory").child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    Log.d("InventoryPage", "No books found in inventory for user: $userId")
                    Toast.makeText(this@InventoryPage, "No books found in inventory for user: $userId", Toast.LENGTH_SHORT).show()
                    bookAdapter.submitBooks(bookList)
                    return
                }

                Log.d("InventoryPage", "Found ${snapshot.childrenCount} book IDs in inventory")
                Toast.makeText(this@InventoryPage, "Found ${snapshot.childrenCount} book IDs", Toast.LENGTH_SHORT).show()
                val totalBooks = snapshot.childrenCount.toInt()
                var booksProcessed = 0

                snapshot.children.forEach { bookSnapshot ->
                    val bookId = bookSnapshot.key ?: return@forEach
                    Log.d("InventoryPage", "Processing bookId: $bookId")
                    Toast.makeText(this@InventoryPage, "Processing bookId: $bookId", Toast.LENGTH_SHORT).show()
                    // Fetch book details from /book/{bookid}
                    database.child("book").child(bookId).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(bookSnapshot: DataSnapshot) {
                            if (bookSnapshot.exists()) {
                                val image = bookSnapshot.child("image").getValue(String::class.java)
                                val name = bookSnapshot.child("name").getValue(String::class.java)
                                val author = bookSnapshot.child("author").getValue(String::class.java)
                                val description = bookSnapshot.child("description").getValue(String::class.java)
                                val categories = bookSnapshot.child("categories").children.mapNotNull { it.getValue(String::class.java) }
                                bookList.add(InventoryBook(bookId, image, name, author, description, categories))
                                Log.d("InventoryPage", "Added book to list: $name")
                                Toast.makeText(this@InventoryPage, "Added book to list: $name", Toast.LENGTH_SHORT).show()
                                // Add a Toast to verify bookList content
                                Toast.makeText(this@InventoryPage, "Current bookList size: ${bookList.size}", Toast.LENGTH_SHORT).show()
                            } else {
                                Log.w("InventoryPage", "Book not found in /book for bookId: $bookId")
                                Toast.makeText(this@InventoryPage, "Book not found in /book for bookId: $bookId", Toast.LENGTH_SHORT).show()
                            }

                            booksProcessed++
                            if (booksProcessed == totalBooks) {
                                Log.d("InventoryPage", "All books processed, updating adapter with ${bookList.size} books")
                                Toast.makeText(this@InventoryPage, "All books processed, updating adapter with ${bookList.size} books", Toast.LENGTH_SHORT).show()
                                // Add a Toast to confirm adapter update
                                Toast.makeText(this@InventoryPage, "Adapter updated with ${bookList.size} items", Toast.LENGTH_SHORT).show()
                                bookAdapter.submitBooks(bookList)
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("InventoryPage", "Failed to load book $bookId: ${error.message}")
                            Toast.makeText(this@InventoryPage, "Failed to load book $bookId: ${error.message}", Toast.LENGTH_SHORT).show()
                            booksProcessed++
                            if (booksProcessed == totalBooks) {
                                Log.d("InventoryPage", "All books processed (with errors), updating adapter with ${bookList.size} books")
                                Toast.makeText(this@InventoryPage, "All books processed (with errors), updating adapter with ${bookList.size} books", Toast.LENGTH_SHORT).show()
                                bookAdapter.submitBooks(bookList)
                            }
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("InventoryPage", "Failed to load inventory: ${error.message}")
                Toast.makeText(this@InventoryPage, "Failed to load inventory: ${error.message}", Toast.LENGTH_SHORT).show()
                if (error.code == DatabaseError.PERMISSION_DENIED) {
                    Toast.makeText(this@InventoryPage, "Permission denied. Please log in again.", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@InventoryPage, LogInPage::class.java))
                    finish()
                } else {
                    Toast.makeText(this@InventoryPage, "Failed to load inventory: ${error.message}", Toast.LENGTH_SHORT).show()
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

            // Split categories by comma and trim whitespace
            val categories = categoriesInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }

            // Generate a new book ID
            val bookId = database.child("book").push().key ?: return@setOnClickListener

            // Create book data
            val bookData = mapOf(
                "image" to (if (imageQuery.isNotEmpty()) imageQuery else null),
                "name" to name,
                "author" to (if (author.isNotEmpty()) author else null),
                "description" to (if (description.isNotEmpty()) description else null),
                "categories" to categories
            )

            // Save the book to /book/{bookid}
            database.child("book").child(bookId).setValue(bookData)
                .addOnSuccessListener {
                    Log.d("InventoryPage", "Book added to /book with ID: $bookId")
                    // Add the book to the user's inventory
                    database.child("Inventory").child(userId).child(bookId).setValue(true)
                        .addOnSuccessListener {
                            Log.d("InventoryPage", "Book added to /Inventory/$userId/$bookId")
                            Toast.makeText(this, "Book added successfully", Toast.LENGTH_SHORT).show()
                            loadInventory(userId) // Refresh the list
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

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}