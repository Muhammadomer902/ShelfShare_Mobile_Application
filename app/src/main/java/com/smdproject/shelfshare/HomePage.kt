package com.smdproject.shelfshare

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.TranslateAnimation
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class HomePage : AppCompatActivity() {
    private lateinit var rootLayout: RelativeLayout
    private lateinit var headerLayout: RelativeLayout
    private lateinit var logoImageView: ImageView
    private lateinit var menuIcon: ImageView
    private lateinit var searchIcon: ImageView
    private lateinit var bookItem1: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home_page)

        // Handle window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize views
        rootLayout = findViewById(R.id.main)
        headerLayout = findViewById(R.id.header)
        logoImageView = findViewById(R.id.logoImageView)
        menuIcon = findViewById(R.id.menu_icon)
        searchIcon = findViewById(R.id.search_icon)
        bookItem1 = findViewById(R.id.book_item_1)

        // Set initial state for animation (white header and orange logo)
        headerLayout.setBackgroundColor(android.graphics.Color.WHITE)
        logoImageView.setImageResource(R.drawable.logo_orange_right)

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

                // Change logo to white_right after animation
                logoImageView.setImageResource(R.drawable.logo_white_right)
            }

            override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
        })

        // Set click listeners for navigation with exit animation
        menuIcon.setOnClickListener {
            // Fade out and slide out to the right
            val fadeOut = AlphaAnimation(1f, 0f)
            fadeOut.duration = 2000
            val slideOutToRight = TranslateAnimation(
                0f, 1000f,  // Slide to the right by 1000px
                0f, 0f
            )
            slideOutToRight.duration = 2000
            val outAnimationSet = AnimationSet(true)
            outAnimationSet.addAnimation(fadeOut)
            outAnimationSet.addAnimation(slideOutToRight)

            outAnimationSet.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                override fun onAnimationStart(animation: android.view.animation.Animation?) {
                    // Disable the view during animation to prevent interaction
                    rootLayout.isEnabled = false
                }

                override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}

                override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                    // Hide the layout to prevent reappearance
                    rootLayout.visibility = View.GONE
                    val intent = Intent(this@HomePage, MenuPage::class.java)
                    startActivity(intent)
                    finish() // Finish after starting the new activity
                }
            })

            rootLayout.startAnimation(outAnimationSet)
            Toast.makeText(this, "Navigating to MenuPage", Toast.LENGTH_SHORT).show()
        }

        searchIcon.setOnClickListener {
            // Fade out and slide out to the right
            val fadeOut = AlphaAnimation(1f, 0f)
            fadeOut.duration = 2000
            val slideOutToRight = TranslateAnimation(
                0f, 1000f,  // Slide to the right by 1000px
                0f, 0f
            )
            slideOutToRight.duration = 2000
            val outAnimationSet = AnimationSet(true)
            outAnimationSet.addAnimation(fadeOut)
            outAnimationSet.addAnimation(slideOutToRight)

            outAnimationSet.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                override fun onAnimationStart(animation: android.view.animation.Animation?) {
                    // Disable the view during animation to prevent interaction
                    rootLayout.isEnabled = false
                }

                override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}

                override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                    // Hide the layout to prevent reappearance
                    rootLayout.visibility = View.GONE
                    val intent = Intent(this@HomePage, SearchPage::class.java)
                    startActivity(intent)
                    finish() // Finish after starting the new activity
                }
            })

            rootLayout.startAnimation(outAnimationSet)
            Toast.makeText(this, "Navigating to SearchPage", Toast.LENGTH_SHORT).show()
        }

        // Set click listener for the first book
        bookItem1.setOnClickListener {
            // Fade out and slide out to the right
            val fadeOut = AlphaAnimation(1f, 0f)
            fadeOut.duration = 2000
            val slideOutToRight = TranslateAnimation(
                0f, 1000f,  // Slide to the right by 1000px
                0f, 0f
            )
            slideOutToRight.duration = 2000
            val outAnimationSet = AnimationSet(true)
            outAnimationSet.addAnimation(fadeOut)
            outAnimationSet.addAnimation(slideOutToRight)

            outAnimationSet.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                override fun onAnimationStart(animation: android.view.animation.Animation?) {
                    // Disable the view during animation to prevent interaction
                    rootLayout.isEnabled = false
                }

                override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}

                override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                    // Hide the layout to prevent reappearance
                    rootLayout.visibility = View.GONE
                    val intent = Intent(this@HomePage, ViewBookPage::class.java).apply {
                        putExtra("book_title", "Book Title 1")
                        putExtra("book_author", "Author Name")
                        putExtra("book_cover", R.drawable.fiction_image)
                    }
                    startActivity(intent)
                    finish() // Finish after starting the new activity
                }
            })

            rootLayout.startAnimation(outAnimationSet)
            Toast.makeText(this, "Navigating to ViewBookPage", Toast.LENGTH_SHORT).show()
        }
    }
}