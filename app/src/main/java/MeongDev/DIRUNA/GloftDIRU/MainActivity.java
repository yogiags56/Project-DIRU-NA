package MeongDev.DIRUNA.GloftDIRU;

import MeongDev.DIRUNA.GloftDIRU.core.LogicalViewport;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;

/**
 * Temporary launcher shell for the native project foundation.
 * Gameplay implementation is intentionally not part of DEV-001B.
 */
public final class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LogicalViewport viewport = LogicalViewport.INITIAL_PORTRAIT;
        TextView status = new TextView(this);
        status.setText("Diamond Rush\nNative Android foundation\nLogical viewport: "
                + viewport.width() + " × " + viewport.height());
        status.setGravity(Gravity.CENTER);
        status.setTextSize(18);
        setContentView(status);
    }
}
