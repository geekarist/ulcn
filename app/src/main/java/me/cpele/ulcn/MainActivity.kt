package me.cpele.ulcn

import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import java.net.URLDecoder

const val LOG_TAG = "202201031454"

class MainActivity : AppCompatActivity() {
    @SuppressLint("QueryPermissionsNeeded")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val headerTextView = findViewById<TextView>(R.id.main_links_found)
        headerTextView.text = getString(R.string.main_links_found)

        val textView = findViewById<TextView>(R.id.main_result)

        val uri = intent.data
            ?: intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
        textView.text = textViewContents(this, uri.toString())
        textView.movementMethod = LinkMovementMethod.getInstance()

        findViewById<Button>(R.id.main_paste_button).setOnClickListener {
            val clipboardManager: ClipboardManager? = getSystemService()
            val clipboard = clipboardManager
                ?.primaryClip
                ?.getItemAt(0)
                ?.text
                .toString()
            textView.text = textViewContents(this, clipboard)
        }
    }
}

fun textViewContents(
    context: Context,
    encodedUri: String
): CharSequence {

    @Suppress("DEPRECATION")
    val decodedUri = URLDecoder.decode(encodedUri)
    Log.d(LOG_TAG, "decodedUri: $decodedUri")
    val q = decodedUri.replace("geo:0,0?q=", "")
    Log.d(LOG_TAG, "q: $q")
    val words = q.split(' ', '\n', '\t', '\r')

    // Build list of clickable spans, one for each managed link of input
    val spans = words.asSequence()
        .map { link -> convertToIntent(link) }
        .filter { intent ->
            isIntentManaged(intent, context.packageManager).also {
                Log.d(LOG_TAG, "Intent managed? intent=($intent) → $it")
            }
        }
        .map { intent ->
            val span = clickableSpan(context, intent)
            span to intent
        }.map { (span, intent) ->
            applySpanToIntent(span, intent)
        }.toList()

    // Trim last item
    val lastItemTrimmed = spans.lastOrNull()?.trim()
    val trimmedSpans = spans.dropLast(1) + lastItemTrimmed

    // Concat clickable spans
    val arrayOfSpans = trimmedSpans.toTypedArray()
    val result = TextUtils.concat(*arrayOfSpans)

    // Return clickable spans if any, or a fallback
    fun getFallback() = context.getString(R.string.main_empty_spans_fallback)
    fun isResultBlank() = result.isNullOrBlank()
    return if (isResultBlank()) getFallback() else result
}

fun convertToIntent(link: String) =
    Intent()
        .setAction(Intent.ACTION_VIEW)
        .setData(Uri.parse(link))

@SuppressLint("QueryPermissionsNeeded")
fun isIntentManaged(intent: Intent, packageManager: PackageManager) =
    packageManager
        .queryIntentActivities(intent, PackageManager.MATCH_ALL)
        .isNotEmpty()

fun clickableSpan(context: Context, intent: Intent): ClickableSpan {

    fun openIntent() {
        Log.d(LOG_TAG, "Opening intent $intent")
        context.startActivity(intent)
    }

    fun fallbackReasonMsg() = context.getString(R.string.main_reason_unknown)
    fun reasonMsg(e: Exception) = e.message ?: fallbackReasonMsg()
    fun errorMsg(e: Exception) = context.getString(
        R.string.main_error_opening_link,
        reasonMsg(e)
    )

    fun showError(e: Exception) = Toast.makeText(
        context,
        errorMsg(e),
        Toast.LENGTH_LONG
    ).show()

    return object : ClickableSpan() {
        override fun onClick(widget: View) {
            try {
                openIntent()
            } catch (e: Exception) {
                showError(e)
            }
        }
    }
}

@Suppress("DEPRECATION")
fun applySpanToIntent(
    span: Any,
    intent: Intent
): CharSequence =
    SpannableStringBuilder(intent.data.toString())
        .apply {
            setSpan(
                span,
                0,
                intent.data.toString().length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            appendln()
            appendln()
        }

