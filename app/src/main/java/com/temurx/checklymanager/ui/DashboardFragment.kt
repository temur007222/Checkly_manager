package com.temurx.checklymanager.ui

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.temurx.checklymanager.R
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.components.Legend
import androidx.core.graphics.toColorInt
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.temurx.checklymanager.data.Staff
import com.temurx.checklymanager.data.Task
import com.temurx.checklymanager.databinding.FragmentDashboardBinding
import com.temurx.checklymanager.utils.StaffAdapter
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: StaffAdapter
    private val staffList = mutableListOf<Staff>()
    private var staffListener: ListenerRegistration? = null

    @RequiresApi(Build.VERSION_CODES.O)
    private val dateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneId.systemDefault())

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)

        adapter = StaffAdapter(staffList) { staff ->
            val bundle = Bundle().apply { putString("staffId", staff.staffId) }
            findNavController().navigate(R.id.staffDetailFragment, bundle)
        }

        binding.staffRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.staffRecycler.adapter = adapter

        fetchStaff()
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupFilter()
        setupBarChart()
        loadChartData("Monthly")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupFilter() {
        val filters = listOf(
            binding.dailyFilter to "Daily",
            binding.weeklyFilter to "Weekly",
            binding.monthlyFilter to "Monthly",
            binding.customFilter to "Custom"
        )

        filters.forEach { (textView, period) ->
            textView.setOnClickListener {
                filters.forEach { (tv, _) ->
                    tv.background = null
                    tv.setTextColor(Color.parseColor("#DEB3B3"))
                }
                textView.setBackgroundResource(R.drawable.edit_text_task_detail)
                textView.setTextColor(Color.BLACK)
                loadChartData(period)
            }
        }

        // Default selection
        val default = binding.monthlyFilter
        default.setBackgroundResource(R.drawable.edit_text_task_detail)
        default.setTextColor(Color.BLACK)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadChartData(filter: String, customStart: LocalDate? = null, customEnd: LocalDate? = null) {
        db.collectionGroup("tasks")
            .get()
            .addOnSuccessListener { snapshot ->
                if (_binding == null || !isAdded) return@addOnSuccessListener

                val today = LocalDate.now()
                val startDate: LocalDate
                val endDate: LocalDate

                when (filter) {
                    "Daily" -> {
                        startDate = today
                        endDate = today
                    }
                    "Weekly" -> {
                        startDate = today.with(DayOfWeek.MONDAY)
                        endDate = today.with(DayOfWeek.SUNDAY)
                    }
                    "Monthly" -> {
                        startDate = today.withDayOfMonth(1)
                        endDate = today.withDayOfMonth(today.lengthOfMonth())
                    }
                    else -> {
                        startDate = customStart ?: today
                        endDate = customEnd ?: today
                    }
                }

                val statusCount = mutableMapOf(
                    "NOT YET AVAILABLE" to 0,
                    "AVAILABLE" to 0,
                    "IN PROGRESS" to 0,
                    "OVERDUE" to 0,
                    "FINISHED" to 0
                )

                for (doc in snapshot.documents) {
                    val task = doc.toObject(Task::class.java) ?: continue
                    val dueDate = task.dueTime?.toDate()?.toInstant()
                        ?.atZone(ZoneId.systemDefault())
                        ?.toLocalDate() ?: continue

                    if (dueDate.isBefore(startDate) || dueDate.isAfter(endDate)) continue
                    val status = task.status ?: "UNKNOWN"
                    statusCount[status] = (statusCount[status] ?: 0) + 1
                }

                updateBarChart(statusCount)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error loading tasks", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupBarChart() {
        val barChart = binding.barChart
        barChart.apply {
            setDrawGridBackground(false)
            setDrawBorders(false)
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(false)
            setScaleEnabled(false)
            extraBottomOffset = (25f)

            // X Axis
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                setDrawAxisLine(false)
                granularity = 1f
                textSize = 10f
                textColor = Color.parseColor("#7A5C5C")
                labelRotationAngle = -45f
            }

            // Y Axis
            axisLeft.apply {
                axisMinimum = 0f
                axisMaximum = 100f
                granularity = 20f
                setDrawGridLines(false)
                setDrawAxisLine(false)
                textSize = 12f
                valueFormatter = PercentFormatter()
            }
            axisRight.isEnabled = false
        }
    }

    private fun updateBarChart(statusCount: Map<String, Int>) {
        val total = statusCount.values.sum()
        if (total == 0) {
            binding.barChart.clear()
            binding.barChart.setNoDataText("No tasks for selected period")
            return
        }

        val statuses = listOf("NOT YET AVAILABLE", "AVAILABLE", "IN PROGRESS", "OVERDUE", "FINISHED")
        val labels = listOf("Not Avail", "Avail", "In Prog", "Overdue", "Finished") // X-axis labels
        val entries = ArrayList<BarEntry>()

        statuses.forEachIndexed { index, status ->
            val count = statusCount[status] ?: 0
            val percent = if (total > 0) count * 100f / total else 0f
            entries.add(BarEntry(index.toFloat(), percent))
        }

        val dataSet = BarDataSet(entries, "").apply {
            color = Color.parseColor("#FF3B30")
            valueTextColor = Color.BLACK
            valueTextSize = 12f
            setDrawValues(true)
            valueFormatter = PercentFormatter()
        }

        val data = BarData(dataSet)
        data.barWidth = 0.7f

        binding.barChart.apply {
            this.data = data
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            setFitBars(true)
            animateY(800)
            invalidate()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchStaff() {

        staffListener = db.collection("staff_list")
            .addSnapshotListener { snapshot, e ->
                if (_binding == null) return@addSnapshotListener  // safe check

                if (e != null) {
                    Log.e("Firestore", "Error fetching staff: ", e)
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    staffList.clear()

                    for (doc in snapshot.documents) {
                        val staff = doc.toObject(Staff::class.java)?.copy(staffId = doc.id)
                        if (staff != null) {
                            // Fetch tasks for this staff
                            db.collection("staff_task")
                                .document(staff.staffId)
                                .collection("tasks")
                                .get()
                                .addOnSuccessListener { tasksSnapshot ->
                                    if (_binding == null) return@addOnSuccessListener

                                    val tasks = tasksSnapshot.toObjects(Task::class.java)
                                    val now = System.currentTimeMillis()

                                    staff.totalTask = tasks.size
                                    staff.overdueCount = tasks.count { task ->
                                        val dueTimeMillis = try {
                                            task.dueTime.toDate().time
                                        } catch (ex: Exception) {
                                            0L
                                        }
                                        !task.isCompleted && dueTimeMillis < now
                                    }
                                    staffList.add(staff)
                                    adapter.notifyDataSetChanged()
                                }
                                .addOnFailureListener { ex ->
                                    Log.e(
                                        "Firestore",
                                        "Error loading tasks for ${staff.fullName}",
                                        ex
                                    )
                                }
                        }
                    }
                } else {
                    Log.w("Firestore", "No staff data found")
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        staffListener?.remove()
    }
}
