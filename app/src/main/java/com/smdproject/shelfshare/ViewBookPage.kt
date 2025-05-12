package com.smdproject.shelfshare

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.TranslateAnimation
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.ArgbEvaluator
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.google.firebase.database.*

class ViewBookPage : AppCompatActivity() {
    private lateinit var rootLayout: RelativeLayout
    private lateinit var headerLayout: RelativeLayout
    private lateinit var logoImageView: ImageView
    private lateinit var menuIcon: ImageView
    private lateinit var searchIcon: ImageView
    private lateinit var bookImage: ImageView
    private lateinit var bookName: TextView
    private lateinit var bookAuthor: TextView
    private lateinit var bookDescription: TextView
    private lateinit var bookCategories: TextView
    private lateinit var barterButton: Button
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_view_book_page)

        // Handle window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase
        database = FirebaseDatabase.getInstance().reference

        // Initialize views
        rootLayout = findViewById(R.id.main)
        headerLayout = findViewById(R.id.header)
        logoImageView = findViewById(R.id.logoImageView)
        menuIcon = findViewById(R.id.menu_icon)
        searchIcon = findViewById(R.id.search_icon)
        bookImage = findViewById(R.id.bookImage)
        bookName = findViewById(R.id.bookName)
        bookAuthor = findViewById(R.id.bookAuthor)
        bookDescription = findViewById(R.id.bookDescription)
        bookCategories = findViewById(R.id.bookCategories)
        barterButton = findViewById(R.id.barterButton)

        // Get bookId from Intent
        val bookId = intent.getStringExtra("book_id") ?: run {
            Toast.makeText(this, "Book ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Fetch book details from Firebase
        database.child("book").child(bookId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(this@ViewBookPage, "Book not found", Toast.LENGTH_SHORT).show()
                    finish()
                    return
                }

                val image = snapshot.child("image").getValue(String::class.java)
                val name = snapshot.child("name").getValue(String::class.java) ?: "Unknown Title"
                val author = snapshot.child("author").getValue(String::class.java) ?: "Unknown Author"
                val description = snapshot.child("description").getValue(String::class.java) ?: "No description available"
                val categories = snapshot.child("categories").children.mapNotNull { it.getValue(String::class.java) }.joinToString(", ")

                // Load book image
                val imageQuery = image?.takeIf { it.isNotEmpty() } ?: "$name book cover"
                val usiUrl = "https://source.unsplash.com/150x200/?$imageQuery"
                Glide.with(this@ViewBookPage)
                    .load(usiUrl)
                    .placeholder(R.drawable.default_book_cover)
                    .into(bookImage)

                // Populate text views
                bookName.text = name
                bookAuthor.text = author
                bookDescription.text = description
                bookCategories.text = if (categories.isNotEmpty()) categories else "No categories"
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ViewBookPage, "Failed to load book: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

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
                    val intent = Intent(this@ViewBookPage, targetActivity)
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

        // Set click listener for Barter button
        barterButton.setOnClickListener {
            applyExitAnimationAndNavigate(InventoryPage::class.java, "Navigating to InventoryPage")
        }
    }
}