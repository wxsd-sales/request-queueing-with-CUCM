package com.ciscowebex.androidsdk.kitchensink.calling.participants

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ciscowebex.androidsdk.kitchensink.R
import com.ciscowebex.androidsdk.kitchensink.databinding.ParticipantsHeaderItemBinding
import com.ciscowebex.androidsdk.kitchensink.databinding.ParticipantsListItemBinding
import com.ciscowebex.androidsdk.phone.CallMembership

class ParticipantsAdapter(private val participants: ArrayList<Any>, private val itemClickListener: OnItemActionListener, private val selfId: String) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val viewTypeHeader = 0
    private val viewTypeParticipant = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType) {
            viewTypeHeader -> {
                HeaderViewHolder(ParticipantsHeaderItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }

            viewTypeParticipant -> {
                ParticipantViewHolder(ParticipantsListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            else -> {
                ParticipantViewHolder(ParticipantsListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (participants[position] is String) {
            viewTypeHeader
        } else viewTypeParticipant
    }

    override fun getItemCount(): Int {
       return participants.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if(participants[position] is String) {
            (holder as HeaderViewHolder).bind()
        } else {
            (holder as ParticipantViewHolder).bind()
        }
    }

    fun refreshData(list: List<Any>) {
        participants.clear()
        participants.addAll(list)
    }

    inner class ParticipantViewHolder(private val binding: ParticipantsListItemBinding): RecyclerView.ViewHolder(binding.root){

        fun bind(){
            val participant = participants[adapterPosition] as CallMembership
            binding.tvName.text = participant.getDisplayName()
            binding.imgMute.setImageResource(R.drawable.ic_mic_off_24)
            binding.imgMute.visibility = if(!participant.isSendingAudio()) View.VISIBLE else View.INVISIBLE

            val personId = participant.getPersonId()

            if (personId == selfId) {
                binding.infoLabelView.visibility = View.VISIBLE
            }
            else {
                binding.infoLabelView.visibility = View.GONE
            }
            binding.root.setOnClickListener { itemClickListener.onParticipantMuted(personId)}
            binding.root.setOnLongClickListener {
                itemClickListener.onLetInClicked(participant)
                true
            }

        }
    }

    inner class HeaderViewHolder(private val binding: ParticipantsHeaderItemBinding): RecyclerView.ViewHolder(binding.root){

        fun bind(){
            binding.tvName.text = participants[adapterPosition] as String
            binding.root.setOnClickListener(null)
        }
    }

    interface OnItemActionListener{
        fun onParticipantMuted(participantId: String)
        fun onLetInClicked(callMembership: CallMembership)
    }
}