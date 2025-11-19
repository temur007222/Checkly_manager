package com.temurx.checklymanager.utils

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.temurx.checklymanager.data.Comment
import com.temurx.checklymanager.databinding.ItemCommentBinding
import java.text.DateFormat

class CommentsAdapter : ListAdapter<Comment, CommentsAdapter.CommentViewHolder>(DiffCallback()) {

    class CommentViewHolder(val binding: ItemCommentBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CommentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = getItem(position)
        holder.binding.commentText.text = comment.text
        holder.binding.commentAuthor.text = comment.authorName
        holder.binding.commentTime.text = DateFormat.getDateTimeInstance().format(comment.createdAt.toDate())
    }

    class DiffCallback : DiffUtil.ItemCallback<Comment>() {
        override fun areItemsTheSame(oldItem: Comment, newItem: Comment) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Comment, newItem: Comment) = oldItem == newItem
    }
}

