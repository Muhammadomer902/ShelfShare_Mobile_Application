package com.smdproject.shelfshare

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

data class Book(val bookId: String, val image: String?, val name: String?, val author: String?)

class BookAdapter(private var books: List<Book> = emptyList()) : RecyclerView.Adapter<BookAdapter.BookViewHolder>() {

    private var onItemClickListener: ((Book) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.profile_book, parent, false)
        return BookViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val book = books[position]
        holder.bind(book)
    }

    override fun getItemCount(): Int = books.size

    fun submitBooks(newBooks: List<Book>) {
        books = newBooks
        notifyDataSetChanged()
    }

    fun setOnItemClickListener(listener: (Book) -> Unit) {
        onItemClickListener = listener
    }

    inner class BookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val bookImage: ImageView = itemView.findViewById(R.id.bookImage)
        private val bookName: TextView = itemView.findViewById(R.id.bookName)
        private val bookAuthor: TextView = itemView.findViewById(R.id.bookAuthor)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClickListener?.invoke(books[position])
                }
            }
        }

        fun bind(book: Book) {
            // Use Unsplash Source Image API to fetch book cover image based on the book's image query
            val imageQuery = book.image?.takeIf { it.isNotEmpty() } ?: "${book.name} book cover"
            val usiUrl = "https://source.unsplash.com/150x200/?$imageQuery"
            Glide.with(itemView.context)
                .load(usiUrl)
                .placeholder(R.drawable.default_book_cover)
                .into(bookImage)

            bookName.text = book.name?.takeIf { it.isNotEmpty() } ?: "Unknown Title"
            bookAuthor.text = book.author?.takeIf { it.isNotEmpty() } ?: "Unknown Author"
        }
    }
}