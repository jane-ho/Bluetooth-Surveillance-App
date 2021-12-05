package com.makerlab.example.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.appcompat.widget.AppCompatSpinner;

import com.makerlab.example.ui.R;


public class ProtocolSelectSpinner extends AppCompatSpinner {
    public ProtocolSelectSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
        String[] text=context.getResources().getStringArray(R.array.protocol_select_spinner);
        ArrayAdapter<String> adapter =new ArrayAdapter<String>(context,R.layout.protocol_select_spinner,text);
        setAdapter(adapter);
        //setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
    }
}
