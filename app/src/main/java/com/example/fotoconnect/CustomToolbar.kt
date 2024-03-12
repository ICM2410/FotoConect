import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.fotoconnect.R

class CustomToolbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    init {
        // Inflate the layout
        LayoutInflater.from(context).inflate(R.layout.costum_toolbar, this, true)
        // You can set up click listeners for other buttons similarly
    }
}
