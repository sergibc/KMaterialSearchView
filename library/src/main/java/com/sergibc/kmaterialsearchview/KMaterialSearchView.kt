package com.sergibc.kmaterialsearchview

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Parcel
import android.os.Parcelable
import android.speech.RecognizerIntent
import android.text.Editable
import android.text.Selection
import android.text.TextUtils
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.MenuItem
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*

class KMaterialSearchView @JvmOverloads constructor(
    private val mContext: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(mContext, attrs, defStyleAttr),
    Filter.FilterListener {

    companion object {
        val REQUEST_VOICE = 9999
    }

    private var savedState: SavedState? = null

    private var menuItem: MenuItem? = null

    private var searchLayout: View? = null
    private var searchTopBar: RelativeLayout? = null
    private var searchTextView: EditText? = null
    private var backBtn: ImageButton? = null
    private var voiceBtn: ImageButton? = null
    private var emptyBtn: ImageButton? = null
    private var suggestionIcon: Drawable? = null
    private var suggestionsListView: ListView? = null
    private var prefix: String? = null
    private var tintView: View? = null
    private var allowVoiceSearch: Boolean = false
    private var ellipsize = false
    private var submit = false

    private var oldQueryText: CharSequence? = null
    private var userQuery: CharSequence? = null

    private var onQueryChangeListener: OnQueryTextListener? = null
    private var searchViewListener: SearchViewListener? = null

    private var adapter: ListAdapter? = null

    private var clearingFocus: Boolean = false

    private val isVoiceAvailable: Boolean
        get() {
            if (isInEditMode) {
                return true
            }
            val pm = context.packageManager
            val activities = pm.queryIntentActivities(
                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0
            )
            return activities.size == 0
        }

    var isSearchOpen = false
        private set

    init {
        initiateView()
        initStyle(attrs, defStyleAttr)
    }

    private fun initiateView() {
        inflate(context, R.layout.search_view, this)
        searchLayout = findViewById(R.id.search_layout)

        searchLayout?.let {
            searchTopBar = it.findViewById(R.id.search_top_bar) as RelativeLayout
            suggestionsListView = it.findViewById(R.id.suggestion_list) as ListView
            searchTextView = it.findViewById(R.id.searchTextView) as EditText
            backBtn = it.findViewById(R.id.action_up_btn) as ImageButton
            voiceBtn = it.findViewById(R.id.action_voice_btn) as ImageButton
            emptyBtn = it.findViewById(R.id.action_empty_btn) as ImageButton
            tintView = it.findViewById(R.id.transparent_view)
        }

        searchTextView?.setOnClickListener { showSuggestions() }
        backBtn?.setOnClickListener { closeSearch() }
        voiceBtn?.setOnClickListener { onVoiceClicked() }
        emptyBtn?.setOnClickListener { clearSearchField() }
        tintView?.setOnClickListener { closeSearch() }

        allowVoiceSearch = false

        showVoice(true)

        initSearchView()

        suggestionsListView?.visibility = View.GONE
    }

    private fun initSearchView() {
        searchTextView?.setOnEditorActionListener { _, _, _ ->
            onSubmitQuery()
            true
        }

        searchTextView?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                userQuery = s
                startFilter(getQueryText(s.toString()))
                this@KMaterialSearchView.onTextChanged(s)
            }

            override fun afterTextChanged(s: Editable) {
                if (prefix != null && s.toString().isNotEmpty()
                    && !s.toString().contains(prefix!!.trim { it <= ' ' })
                    && !searchTextView?.text.toString().contains(prefix!!.trim { it <= ' ' })
                ) {
                    searchTextView!!.setText("$prefix$s")
                    Selection.setSelection(
                        searchTextView!!.text,
                        searchTextView!!.text.length
                    )
                } else if (prefix != null && searchTextView!!.text.toString() == prefix) {
                    searchTextView!!.text = null
                    Selection.setSelection(
                        searchTextView!!.text,
                        searchTextView!!.text.length
                    )
                }
            }
        })

        searchTextView?.onFocusChangeListener =
            OnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    showKeyboard(searchTextView)
                    showSuggestions()
                }
            }
    }

    private fun initStyle(attrs: AttributeSet?, defStyleAttr: Int) {
        val a =
            context.obtainStyledAttributes(attrs, R.styleable.KMaterialSearchView, defStyleAttr, 0)

        with(a) {
            if (hasValue(R.styleable.KMaterialSearchView_searchBackground)) {
                background = a.getDrawable(R.styleable.KMaterialSearchView_searchBackground)
            }

            if (hasValue(R.styleable.KMaterialSearchView_android_textColor)) {
                setTextColor(a.getColor(R.styleable.KMaterialSearchView_android_textColor, 0))
            }

            if (hasValue(R.styleable.KMaterialSearchView_android_textColorHint)) {
                setHintTextColor(
                    a.getColor(
                        R.styleable.KMaterialSearchView_android_textColorHint,
                        0
                    )
                )
            }

            if (hasValue(R.styleable.KMaterialSearchView_android_hint)) {
                setHint(a.getString(R.styleable.KMaterialSearchView_android_hint))
            }

            if (hasValue(R.styleable.KMaterialSearchView_searchVoiceIcon)) {
                setVoiceIcon(a.getDrawable(R.styleable.KMaterialSearchView_searchVoiceIcon))
            }

            if (hasValue(R.styleable.KMaterialSearchView_searchCloseIcon)) {
                setCloseIcon(a.getDrawable(R.styleable.KMaterialSearchView_searchCloseIcon))
            }

            if (hasValue(R.styleable.KMaterialSearchView_searchBackIcon)) {
                setBackIcon(a.getDrawable(R.styleable.KMaterialSearchView_searchBackIcon))
            }

            if (hasValue(R.styleable.KMaterialSearchView_searchSuggestionBackground)) {
                setSuggestionBackground(a.getDrawable(R.styleable.KMaterialSearchView_searchSuggestionBackground))
            }

            if (hasValue(R.styleable.KMaterialSearchView_searchSuggestionIcon)) {
                setSuggestionIcon(a.getDrawable(R.styleable.KMaterialSearchView_searchSuggestionIcon))
            }

            if (hasValue(R.styleable.KMaterialSearchView_android_inputType)) {
                setInputType(
                    a.getInt(
                        R.styleable.KMaterialSearchView_android_inputType,
                        EditorInfo.TYPE_NULL
                    )
                )
            }

            if (hasValue(R.styleable.KMaterialSearchView_searchFieldPrefix)) {
                setPrefix(a.getString(R.styleable.KMaterialSearchView_searchFieldPrefix))
            }

        }

        a.recycle()
    }

    private fun clearSearchField() {
        searchTextView?.text = null
    }

    private fun onSubmitQuery() {
        val query = getQueryText(searchTextView?.text.toString())
        if (query != null && TextUtils.getTrimmedLength(query) > 0) {
            if (onQueryChangeListener == null || !onQueryChangeListener!!.onQueryTextSubmit(query.toString())) {
                closeSearch()
                searchTextView?.text = null
            }
        }
    }

    private fun getQueryText(full: String): String? {
        return if (prefix != null && full.contains(prefix!!)) {
            full.substring(prefix!!.length)
        } else {
            full
        }
    }

    private fun onTextChanged(newText: CharSequence) {
        val text = getQueryText(searchTextView?.text.toString())
        userQuery = text
        val hasText = !TextUtils.isEmpty(text)
        if (hasText) {
            emptyBtn?.visibility = View.VISIBLE
            showVoice(false)
        } else {
            emptyBtn?.visibility = View.GONE
            showVoice(true)
        }

        if (onQueryChangeListener != null && !TextUtils.equals(newText, oldQueryText)) {
            onQueryChangeListener!!.onQueryTextChange(newText.toString())
        }
        oldQueryText = newText.toString()
    }

    private fun startFilter(s: CharSequence?) {
        if (adapter != null && adapter is Filterable) {
            (adapter as Filterable).filter.filter(s, this@KMaterialSearchView)
        }
    }

    private fun setVisibleWithAnimation() {
        val animationListener = object : AnimationUtil.AnimationListener {
            override fun onAnimationStart(view: View): Boolean {
                return false
            }

            override fun onAnimationEnd(view: View): Boolean {
                searchViewListener?.onSearchViewShown()
                return false
            }

            override fun onAnimationCancel(view: View): Boolean {
                return false
            }
        }

        searchLayout?.visibility = View.VISIBLE
        AnimationUtil.reveal(searchTopBar as View, animationListener)
    }

    private fun onVoiceClicked() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        //intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak an item name or number");    // user hint
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH
        )    // setting recognition model, optimized for short phrases – search queries
        intent.putExtra(
            RecognizerIntent.EXTRA_MAX_RESULTS,
            1
        )    // quantity of results we want to receive
        if (mContext is Activity) {
            mContext.startActivityForResult(intent, REQUEST_VOICE)
        }
    }

    override fun setBackground(background: Drawable?) {
        searchTopBar?.background = background
    }

    override fun onFilterComplete(count: Int) {
        if (count > 0) {
            showSuggestions()
        } else {
            dismissSuggestions()
        }
    }

    override fun requestFocus(direction: Int, previouslyFocusedRect: Rect?): Boolean {
        if (clearingFocus) return false
        return if (!isFocusable) false else {
            previouslyFocusedRect?.let {
                searchTextView?.requestFocus(direction, previouslyFocusedRect) ?: false
            } ?: false
        }
    }

    override fun clearFocus() {
        clearingFocus = true
        hideKeyboard(this)
        super.clearFocus()
        searchTextView?.clearFocus()
        clearingFocus = false
    }

    fun setOnItemClickListener(listener: AdapterView.OnItemClickListener) {
        suggestionsListView?.onItemClickListener = listener
    }

    fun setAdapter(adapter: ListAdapter) {
        this.adapter = adapter
        suggestionsListView?.adapter = adapter
        startFilter(getQueryText(searchTextView?.text.toString()))
    }

    fun setSuggestions(suggestions: Array<String>?) {
        if (suggestions != null && suggestions.isNotEmpty()) {
            tintView?.visibility = View.VISIBLE
            val adapter = SearchAdapter(context, suggestions, suggestionIcon, ellipsize)
            setAdapter(adapter)

            setOnItemClickListener(AdapterView.OnItemClickListener { _, _, position, _ ->
                setQuery(
                    adapter.getItem(position) as String,
                    submit
                )
            })
        } else {
            tintView?.visibility = View.GONE
        }
    }

    fun dismissSuggestions() {
        if (suggestionsListView?.visibility == View.VISIBLE) {
            suggestionsListView?.visibility = View.GONE
        }
    }

    @JvmOverloads
    fun showSearch(animate: Boolean = true) {
        if (isSearchOpen) {
            return
        }

        //Request Focus
        searchTextView?.text = null
        searchTextView?.requestFocus()

        if (animate) {
            setVisibleWithAnimation()

        } else {
            searchLayout?.visibility = View.VISIBLE
            searchViewListener?.onSearchViewShown()
        }
        isSearchOpen = true
    }

    fun closeSearch() {
        if (!isSearchOpen) {
            return
        }

        searchTextView?.text = null
//        dismissSuggestions()
        clearFocus()

        searchLayout?.visibility = View.GONE
        searchViewListener?.onSearchViewClosed()
        isSearchOpen = false

    }

    fun hideKeyboard(view: View) {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun showKeyboard(view: View?) {
        view?.let {
            it.requestFocus()
            val imm =
                it.context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(it, 0)
        }
    }

    fun setQuery(query: CharSequence?, submit: Boolean) {
        searchTextView?.setText(query)
        if (query != null) {
            searchTextView?.setSelection(searchTextView!!.length())
            userQuery = query
        }
        if (submit && !TextUtils.isEmpty(query)) {
            onSubmitQuery()
        }
    }

    fun showVoice(show: Boolean) {
        if (show && isVoiceAvailable && allowVoiceSearch) {
            voiceBtn?.visibility = View.VISIBLE
        } else {
            voiceBtn?.visibility = View.GONE
        }
    }

    fun setMenuItem(menuItem: MenuItem) {
        this.menuItem = menuItem
        menuItem?.setOnMenuItemClickListener {
            showSearch()
            true
        }
    }

    fun showSuggestions() {
        if (adapter != null && adapter!!.count > 0 && suggestionsListView?.visibility == View.GONE) {
            suggestionsListView?.visibility = View.VISIBLE
        }
    }

    fun setTextColor(color: Int) {
        searchTextView?.setTextColor(color)
    }

    fun setHintTextColor(color: Int) {
        searchTextView?.setHintTextColor(color)
    }

    fun setHint(hint: CharSequence?) {
        searchTextView?.hint = hint
    }

    fun setVoiceIcon(drawable: Drawable?) {
        voiceBtn?.setImageDrawable(drawable)
    }

    fun setCloseIcon(drawable: Drawable?) {
        emptyBtn?.setImageDrawable(drawable)
    }

    fun setBackIcon(drawable: Drawable?) {
        backBtn?.setImageDrawable(drawable)
    }

    fun setSuggestionIcon(drawable: Drawable?) {
        suggestionIcon = drawable
    }

    fun setInputType(inputType: Int) {
        searchTextView?.inputType = inputType
    }

    fun setSuggestionBackground(background: Drawable?) {
        suggestionsListView?.background = background
    }

    fun setEllipsize(ellipsize: Boolean) {
        this.ellipsize = ellipsize
    }

    fun setSubmitOnClick(submit: Boolean) {
        this.submit = submit
    }

    fun setPrefix(prefix: String?) {
        this.prefix = "$prefix "
    }

    public override fun onRestoreInstanceState(state: Parcelable) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }

        savedState = state

        if (savedState!!.isSearchOpen) {
            showSearch(false)
            setQuery(savedState!!.query, false)
        }

        super.onRestoreInstanceState(savedState!!.superState)
    }

    fun setOnQueryTextListener(listener: OnQueryTextListener) {
        onQueryChangeListener = listener
    }

    fun setOnSearchViewListener(listener: SearchViewListener) {
        searchViewListener = listener
    }

    internal class SavedState : BaseSavedState {
        var query: String? = null
        var isSearchOpen: Boolean = false

        constructor(superState: Parcelable) : super(superState) {}

        private constructor(`in`: Parcel) : super(`in`) {
            this.query = `in`.readString()
            this.isSearchOpen = `in`.readInt() == 1
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeString(query)
            out.writeInt(if (isSearchOpen) 1 else 0)
        }

        companion object {
            //required field that makes Parcelables from a Parcel
            @SuppressLint("ParcelCreator")
            val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(`in`: Parcel): SavedState {
                    return SavedState(`in`)
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }

    interface OnQueryTextListener {

        /**
         * Called when the user submits the query. This could be due to a key press on the
         * keyboard or due to pressing a submit button.
         * The listener can override the standard behavior by returning true
         * to indicate that it has handled the submit request. Otherwise return false to
         * let the SearchView handle the submission by launching any associated intent.
         *
         * @param query the query text that is to be submitted
         * @return true if the query has been handled by the listener, false to let the
         * SearchView perform the default action.
         */
        fun onQueryTextSubmit(query: String): Boolean

        /**
         * Called when the query text is changed by the user.
         *
         * @param newText the new content of the query text field.
         * @return false if the SearchView should perform the default action of showing any
         * suggestions if available, true if the action was handled by the listener.
         */
        fun onQueryTextChange(newText: String): Boolean
    }

    interface SearchViewListener {
        fun onSearchViewShown()

        fun onSearchViewClosed()
    }


}