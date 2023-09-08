package com.dscvit.vitty.ui.community

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.dscvit.vitty.R
import com.dscvit.vitty.adapter.SearchAdapter
import com.dscvit.vitty.databinding.FragmentRequestsBinding
import com.dscvit.vitty.databinding.FragmentSearchBinding
import com.dscvit.vitty.network.api.community.responses.user.UserResponse
import com.dscvit.vitty.util.Constants
import timber.log.Timber


class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private lateinit var communityViewModel: CommunityViewModel

    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        val root: View = binding.root
        communityViewModel = ViewModelProvider(this)[CommunityViewModel::class.java]
        //get token from shared prefs
        val sharedPreferences = activity?.getSharedPreferences(Constants.USER_INFO, Context.MODE_PRIVATE)
        val token = sharedPreferences?.getString(Constants.COMMUNITY_TOKEN, null)
        binding.searchToolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.close -> {
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                    true
                }
                else -> {
                    false
                }
            }
        }
        val searchEditText = binding.searchFriendsText
        searchEditText.requestFocus()
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT)
        binding.searchFriendsText.setCompoundDrawablesWithIntrinsicBounds(
            R.drawable.ic_search,
            0,
            0,
            0
        )

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                Timber.d("Query2--$token-: $query")
                if(token!=null){
                    Timber.d("Qer---: $query")
                    if(query.isEmpty()){
                        Timber.d("Qur2---: $query")
                        binding.searchList.visibility = View.GONE
                        binding.noSearchResults.visibility = View.VISIBLE
                        binding.searchFriendsText.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.ic_search,
                            0,
                            0,
                            0
                        )
                    }else{
                        Timber.d("Query---: $query")
                        communityViewModel.getSearchResult(token,query)
                        binding.searchFriendsText.setCompoundDrawablesWithIntrinsicBounds(
                            0,
                            0,
                            0,
                            0
                        )
                    }

                }
            }

            override fun afterTextChanged(s: Editable?) {

            }
        })

        communityViewModel.searchResult.observe(viewLifecycleOwner){
            Timber.d("SearchResult: $it")
            if(it!=null){
                val searchResult = removeSelfAndFriends(it)
                binding.apply {
                    if(searchResult.isNotEmpty()){
                        searchList.scheduleLayoutAnimation()
                        searchList.adapter =
                            token?.let { token ->
                                SearchAdapter(searchResult,
                                    token, communityViewModel)
                            }
                        searchList.layoutManager = LinearLayoutManager(context)
                        noSearchResults.visibility = View.INVISIBLE
                        searchList.visibility = View.VISIBLE
                    }else{
                        searchList.visibility = View.GONE
                        noSearchResults.visibility = View.VISIBLE
                    }
                }
            }
        }



        return root
    }

    private fun removeSelfAndFriends(it: List<UserResponse>): List<UserResponse> {
        val filteredList = it.filter { userResponse ->
            userResponse.friend_status != "self" && userResponse.friend_status != "friends"
        }

        return filteredList


    }
}