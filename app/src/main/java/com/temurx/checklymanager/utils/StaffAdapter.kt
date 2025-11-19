package com.temurx.checklymanager.utils

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.temurx.checklymanager.data.Staff
import com.temurx.checklymanager.databinding.ItemStaffBinding

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
        Glide.with(holder.itemView.context)
            .load(staff.photoUrl)
            .into(holder.binding.staffImage)

        holder.binding.staffName.text = staff.fullName
        holder.binding.staffRole.text = staff.role
        holder.binding.taskInfo.text =
            if (staff.totalTask > 1) {
                "${staff.totalTask} tasks"
            }else
            {
                "${staff.totalTask} task"
            }
        holder.binding.root.setOnClickListener { onClick(staff) }
    }

    override fun getItemCount() = staffList.size
}
