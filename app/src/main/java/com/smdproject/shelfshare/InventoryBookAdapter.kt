package com.smdproject.shelfshare

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.net.URLEncoder

data class InventoryBook(val bookId: String, val image: String?, val name: String?, val author: String?, val description: String?, val categories: List<String>?)

class InventoryBookAdapter(
    private var books: MutableList<InventoryBook> = mutableListOf(),
    private val onBookDeleted: () -> Unit
) : RecyclerView.Adapter<InventoryBookAdapter.BookViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.inventory_book, parent, false)
        return BookViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val book = books[position]
        holder.bind(book)
    }

    override fun getItemCount(): Int = books.size

    fun submitBooks(newBooks: List<InventoryBook>) {
        books.clear()
        books.addAll(newBooks)
        notifyDataSetChanged()
    }

    inner class BookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val bookImage: ImageView = itemView.findViewById(R.id.bookImage)
        private val bookName: TextView = itemView.findViewById(R.id.bookName)
        private val bookAuthor: TextView = itemView.findViewById(R.id.bookAuthor)
        private val bookDescription: TextView = itemView.findViewById(R.id.bookDescription)
        private val bookCategory: TextView = itemView.findViewById(R.id.bookCategory)
        private val deleteButton: Button = itemView.findViewById(R.id.deleteButton)

        fun bind(book: InventoryBook) {
            // Use Unsplash Source Image API to fetch book cover image based on the book's image query
            val imageQuery = book.image?.takeIf { it.isNotEmpty() } ?: "${book.name} book cover"
            val encodedQuery = URLEncoder.encode(imageQuery, "UTF-8").replace("+", "%20")
            val usiUrl = "https://source.unsplash.com/100x150/?$encodedQuery"
            Log.d("InventoryBookAdapter", "Loading image for book '${book.name}' with URL: $usiUrl")

            Glide.with(itemView.context)
                .asBitmap()
                .load(usiUrl)
                .apply(RequestOptions()
                    .placeholder(R.drawable.default_book_cover)
                    .error(R.drawable.default_book_cover))
                .transition(BitmapTransitionOptions.withCrossFade())
                .into(bookImage)

            bookName.text = book.name?.takeIf { it.isNotEmpty() } ?: "Unknown Title"
            bookAuthor.text = book.author?.takeIf { it.isNotEmpty() } ?: "Unknown Author"
            bookDescription.text = book.description?.takeIf { it.isNotEmpty() } ?: "No description available"
            bookCategory.text = book.categories?.joinToString(", ")?.takeIf { it.isNotEmpty() } ?: "No categories"

            deleteButton.setOnClickListener {
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener
                val database = FirebaseDatabase.getInstance().reference

                database.child("Inventory").child(userId).child(book.bookId).removeValue()
                    .addOnSuccessListener {
                        val position = adapterPosition
                        if (position != RecyclerView.NO_POSITION) {
                            books.removeAt(position)
                            notifyItemRemoved(position)
                            onBookDeleted()
                            Toast.makeText(itemView.context, "Book deleted successfully", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { error ->
                        Toast.makeText(itemView.context, "Failed to delete book: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }
}