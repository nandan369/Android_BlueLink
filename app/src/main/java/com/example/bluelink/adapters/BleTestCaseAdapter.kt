package com.example.bluelink.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.bluelink.databinding.ItemTestCaseBinding
import com.example.bluelink.models.BleTestCasesModel

class BleTestCaseAdapter(private val testCases: List<BleTestCasesModel>) :
    RecyclerView.Adapter<BleTestCaseAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemTestCaseBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTestCaseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount() = testCases.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val testCase = testCases[position]
        holder.binding.tvTestTitle.text = testCase.title
        holder.binding.tvTestDescription.text = testCase.description
        holder.binding.checkboxTest.isChecked = testCase.isSelected

        holder.binding.checkboxTest.setOnCheckedChangeListener { _, isChecked ->
            testCase.isSelected = isChecked
        }
    }

    fun getSelectedTestCases(): List<BleTestCasesModel> {
        return testCases.filter { it.isSelected }
    }
}
