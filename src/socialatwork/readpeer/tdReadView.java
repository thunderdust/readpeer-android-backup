package socialatwork.readpeer;

import android.content.Context;
import android.graphics.Color;
import android.text.Layout;
import android.text.Selection;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.EditText;

public class tdReadView extends EditText {
	
	private int off;

	public tdReadView(Context context) {
		super(context);
		initialize();
	}
	
	private void initialize() {
		setGravity(Gravity.TOP);
		setBackgroundColor(Color.WHITE);
	}
	
	@Override 
	protected void onCreateContextMenu(ContextMenu menu) {
		
	}
	
	@Override 
	public boolean getDefaultEditable() {
		return false;
	}
	
	@Override 
	public boolean onTouchEvent(MotionEvent event) {    
        int action = event.getAction();    
        Layout layout = getLayout();    
        int line = 0;    
        switch(action) {    
        case MotionEvent.ACTION_DOWN:    
            line = layout.getLineForVertical(getScrollY()+ (int)event.getY());            
            off = layout.getOffsetForHorizontal(line, (int)event.getX());    
            Selection.setSelection(getEditableText(), off);    
            break;    
        case MotionEvent.ACTION_MOVE:    
        case MotionEvent.ACTION_UP:    
            line = layout.getLineForVertical(getScrollY()+(int)event.getY());     
            int curOff = layout.getOffsetForHorizontal(line, (int)event.getX());                
            Selection.setSelection(getEditableText(), off, curOff);    
            break;    
        }    
        return true;    
    }    
	

}    
