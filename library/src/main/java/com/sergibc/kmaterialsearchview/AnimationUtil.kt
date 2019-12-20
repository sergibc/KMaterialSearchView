package com.sergibc.kmaterialsearchview


import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.util.TypedValue
import android.view.View
import android.view.ViewAnimationUtils
import androidx.core.view.ViewCompat
import androidx.core.view.ViewPropertyAnimatorListener


object AnimationUtil {
    const val ANIMATION_DURATION_SHORT: Long = 150
    const val ANIMATION_DURATION_MEDIUM: Long = 400
    const val ANIMATION_DURATION_LONG: Long = 800

    interface AnimationListener {
        /**
         * @return true to override parent. Else execute Parent method
         */
        fun onAnimationStart(view: View): Boolean

        fun onAnimationEnd(view: View): Boolean

        fun onAnimationCancel(view: View): Boolean
    }

    @JvmOverloads
    fun crossFadeViews(showView: View, hideView: View, duration: Long = ANIMATION_DURATION_SHORT) {
        fadeInView(showView, duration)
        fadeOutView(hideView, duration)
    }

    @JvmOverloads
    fun fadeInView(
        view: View,
        duration: Long = ANIMATION_DURATION_SHORT,
        listener: AnimationListener? = null
    ) {
        view.visibility = View.VISIBLE
        view.alpha = 0f
        var vpListener: ViewPropertyAnimatorListener? = null

        if (listener != null) {
            vpListener = object : ViewPropertyAnimatorListener {
                override fun onAnimationStart(view: View) {
                    if (!listener.onAnimationStart(view)) {
                        view.isDrawingCacheEnabled = true
                    }
                }

                override fun onAnimationEnd(view: View) {
                    if (!listener.onAnimationEnd(view)) {
                        view.isDrawingCacheEnabled = false
                    }
                }

                override fun onAnimationCancel(view: View) {}
            }
        }
        ViewCompat.animate(view).alpha(1f).setDuration(duration).setListener(vpListener)
    }

    fun reveal(view: View, listener: AnimationListener) {
        val cx = view.width - TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 24f, view.resources.displayMetrics
        ).toInt()
        val cy = view.height / 2
        val finalRadius = Math.max(view.width, view.height)

        val anim = ViewAnimationUtils.createCircularReveal(view, cx, cy, 0f, finalRadius.toFloat())
        view.visibility = View.VISIBLE
        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                listener.onAnimationStart(view)
            }

            override fun onAnimationEnd(animation: Animator) {
                listener.onAnimationEnd(view)
            }

            override fun onAnimationCancel(animation: Animator) {
                listener.onAnimationCancel(view)
            }

            override fun onAnimationRepeat(animation: Animator) {

            }
        })
        anim.start()
    }

    @JvmOverloads
    fun fadeOutView(
        view: View,
        duration: Long = ANIMATION_DURATION_SHORT,
        listener: AnimationListener? = null
    ) {
        ViewCompat.animate(view).alpha(0f).setDuration(duration)
            .setListener(object : ViewPropertyAnimatorListener {
                override fun onAnimationStart(view: View) {
                    if (listener == null || !listener.onAnimationStart(view)) {
                        view.isDrawingCacheEnabled = true
                    }
                }

                override fun onAnimationEnd(view: View) {
                    if (listener == null || !listener.onAnimationEnd(view)) {
                        view.visibility = View.GONE
                        view.isDrawingCacheEnabled = false
                    }
                }

                override fun onAnimationCancel(view: View) {}
            })
    }
}