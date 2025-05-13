package com.smdproject.shelfshare

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.database.*
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder

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
    private lateinit var contactButton: Button
    private lateinit var bookOwnerPic: ImageView
    private lateinit var bookOwnerName: TextView
    private lateinit var database: DatabaseReference
    private val client = OkHttpClient()
    private val GOOGLE_API_KEY = "AIzaSyDW-Hweo3zlykmB-PGYtywJOdDVMTjijlk" // Your Google Books API key
    private val TAG = "ViewBookPage"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_view_book_page)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        database = FirebaseDatabase.getInstance().reference

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
        contactButton = findViewById(R.id.contactButton)
        bookOwnerPic = findViewById(R.id.bookOwnerPic)
        bookOwnerName = findViewById(R.id.bookOwnerName)

        val bookId = intent.getStringExtra("book_id") ?: run {
            Toast.makeText(this, "Book ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Fetch book details including ownerId
        database.child("book").child(bookId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(this@ViewBookPage, "Book not found", Toast.LENGTH_SHORT).show()
                    finish()
                    return
                }

                val name = snapshot.child("name").getValue(String::class.java) ?: "Unknown Title"
                val author = snapshot.child("author").getValue(String::class.java) ?: "Unknown Author"
                val description = snapshot.child("description").getValue(String::class.java) ?: "No description available"
                val categories = snapshot.child("categories").children.mapNotNull { it.getValue(String::class.java) }.joinToString(", ")
                val ownerId = snapshot.child("ownerId").getValue(String::class.java)

                Log.d(TAG, "Fetched book: $name, Author: $author, Description: $description, Categories: $categories, OwnerId: $ownerId")

                // Populate book details
                bookName.text = name
                bookAuthor.text = author
                bookDescription.text = description
                bookCategories.text = if (categories.isNotEmpty()) categories else "No categories"

                // Fetch book cover
                fetchBookCoverFromGoogleBooks(name) { imageUrl ->
                    runOnUiThread {
                        if (imageUrl != null) {
                            Log.d(TAG, "Loading image URL: $imageUrl")
                            Glide.with(this@ViewBookPage)
                                .asBitmap()
                                .load(imageUrl)
                                .apply(
                                    RequestOptions()
                                    .placeholder(R.drawable.default_book_cover)
                                    .error(R.drawable.default_book_cover))
                                .transition(BitmapTransitionOptions.withCrossFade())
                                .into(bookImage)
                        } else {
                            Log.w(TAG, "No image URL found for: $name")
                            bookImage.setImageResource(R.drawable.default_book_cover)
                        }
                    }
                }

                // Fetch owner details if ownerId exists
                if (ownerId != null) {
                    database.child("users").child(ownerId).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(userSnapshot: DataSnapshot) {
                            val ownerName = userSnapshot.child("name").getValue(String::class.java) ?: "Unknown User"
                            val ownerPicUrl = userSnapshot.child("profilePublicId").getValue(String::class.java)
                            bookOwnerName.text = ownerName
                            val profileImageUrl = "https://res.cloudinary.com/ddpt74pga/image/upload/$ownerPicUrl"
                            if (ownerPicUrl != null) {
                                Glide.with(this@ViewBookPage)
                                    .load(profileImageUrl)
                                    .placeholder(R.drawable.default_profile_pic)
                                    .error(R.drawable.default_profile_pic)
                                    .into(bookOwnerPic)
                            } else {
                                bookOwnerPic.setImageResource(R.drawable.default_profile_pic)
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e(TAG, "Failed to load owner: ${error.message}")
                            bookOwnerName.text = "Unknown User"
                            bookOwnerPic.setImageResource(R.drawable.default_profile_pic)
                        }
                    })
                } else {
                    bookOwnerName.text = "Unknown User"
                    bookOwnerPic.setImageResource(R.drawable.default_profile_pic)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ViewBookPage, "Failed to load book: ${error.message}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Firebase error: ${error.message}")
            }
        })

        // Set up UI animations
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

        // Navigation setup
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
                    startActivity(Intent(this@ViewBookPage, targetActivity))
                    finish()
                }
            })
            rootLayout.startAnimation(outAnimationSet)
            Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show()
        }

        menuIcon.setOnClickListener { applyExitAnimationAndNavigate(MenuPage::class.java, "Navigating to MenuPage") }
        searchIcon.setOnClickListener { applyExitAnimationAndNavigate(SearchPage::class.java, "Navigating to SearchPage") }
        barterButton.setOnClickListener { applyExitAnimationAndNavigate(InventoryPage::class.java, "Navigating to InventoryPage") }
        contactButton.setOnClickListener { applyExitAnimationAndNavigate(ChatPage::class.java, "Navigating to ChatPage") }
    }

    private fun fetchBookCoverFromGoogleBooks(title: String, callback: (String?) -> Unit) {
        val encodedTitle = URLEncoder.encode(title, "UTF-8").replace("+", "%20")
        val url = "https://www.googleapis.com/books/v1/volumes?q=intitle:$encodedTitle&key=$GOOGLE_API_KEY"
        Log.d(TAG, "Fetching book cover from: $url")

        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "API call failed: ${e.message}")
                runOnUiThread { callback(null) }
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        Log.d(TAG, "API response: $responseBody")
                        if (responseBody.isNullOrEmpty()) {
                            Log.w(TAG, "Empty response body")
                            runOnUiThread { callback(null) }
                            return
                        }

                        val json = JSONObject(responseBody)
                        val items = json.optJSONArray("items")
                        if (items == null || items.length() == 0) {
                            Log.w(TAG, "No items found for title: $title")
                            runOnUiThread { callback(null) }
                            return
                        }

                        val firstItem = items.getJSONObject(0)
                        val volumeInfo = firstItem.optJSONObject("volumeInfo")
                        val imageLinks = volumeInfo?.optJSONObject("imageLinks")
                        val thumbnail = imageLinks?.optString("thumbnail")?.replace("http://", "https://")
                        Log.d(TAG, "Thumbnail URL: $thumbnail")
                        runOnUiThread { callback(thumbnail) }
                    } else {
                        Log.e(TAG, "Unsuccessful response: ${response.code} - ${response.body?.string()}")
                        runOnUiThread { callback(null) }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing response: ${e.message}")
                    runOnUiThread { callback(null) }
                } finally {
                    response.close()
                }
            }
        })
    }
}