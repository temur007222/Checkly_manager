package com.temurx.checklymanager.utils

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.temurx.checklymanager.R
import com.temurx.checklymanager.data.Staff
import com.temurx.checklymanager.databinding.ItemStaffBinding
import kotlin.math.absoluteValue

class StaffAdapter(
    private val staffList: List<Staff>,
    private val onClick: (Staff) -> Unit
) : RecyclerView.Adapter<StaffAdapter.StaffViewHolder>() {

    inner class StaffViewHolder(val binding: ItemStaffBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StaffViewHolder {
        val binding = ItemStaffBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StaffViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: StaffViewHolder, position: Int) {
        val staff = staffList[position]
        val ctx = holder.itemView.context

        // Avatar — tint deterministic from staffId; load remote photo if non-default
        val tintRes = when (staff.staffId.hashCode().absoluteValue % 3) {
            0 -> R.drawable.bg_avatar_a
            1 -> R.drawable.bg_avatar_b
            else -> R.drawable.bg_avatar_c
        }
        holder.binding.staffImage.setBackgroundResource(tintRes)
        if (staff.photoUrl.isNotBlank() && !staff.photoUrl.contains("pinimg.com")) {
            Glide.with(ctx)
                .load(staff.photoUrl)
                .centerCrop()
                .into(holder.binding.staffImage)
        } else {
            holder.binding.staffImage.setImageDrawable(null)
        }

        holder.binding.staffName.text = staff.fullName
        holder.binding.staffRole.text = staff.role
        holder.binding.taskInfo.text = staff.totalTask.toString()

        if (staff.overdueCount > 0) {
            holder.binding.staffOverdueFlag.visibility = View.VISIBLE
            holder.binding.staffOverdueFlag.text =
                ctx.getString(R.string.home_overdue_flag, staff.overdueCount)
        } else {
            holder.binding.staffOverdueFlag.visibility = View.GONE
        }

        holder.binding.root.setOnClickListener { onClick(staff) }
    }

    override fun getItemCount() = staffList.size
}
