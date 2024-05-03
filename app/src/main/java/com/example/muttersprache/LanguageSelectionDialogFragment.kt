import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import java.util.Locale

class LanguageSelectionDialogFragment : DialogFragment() {

    private var listener: OnLanguageSelectedListener? = null

    fun setOnLanguageSelectedListener(listener: OnLanguageSelectedListener) {
        this.listener = listener
    }

    interface OnLanguageSelectedListener {
        fun onLanguageSelected(language: Locale)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val languages = arrayOf("English", "German") // Список доступных языков
        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle("Select Language")
            .setItems(languages, DialogInterface.OnClickListener { dialog, which ->
                // Получить выбранный язык и вызвать соответствующий обратный вызов
                val selectedLanguage = when (which) {
                    0 -> Locale.ENGLISH
                    1 -> Locale.GERMAN
                    else -> Locale.ENGLISH // По умолчанию английский
                }
                listener?.onLanguageSelected(selectedLanguage)
            })
        return builder.create()
    }
}
