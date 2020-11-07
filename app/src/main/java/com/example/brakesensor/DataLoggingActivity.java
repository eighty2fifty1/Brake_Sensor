package com.example.brakesensor;

import android.graphics.Color;
import android.os.Bundle;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.DefaultAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.View;

import com.example.brakesensor.databinding.ActivityDataLoggingBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

public class DataLoggingActivity extends AppCompatActivity {
    private LineChart lineChart;
    private ActivityDataLoggingBinding binding;

    private boolean clicked =  true;
    private static final String TAG = DataLoggingActivity.class.getSimpleName();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDataLoggingBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        lineChart = binding.activityMainLinechart;


        configureLineChart();
        binding.activityMainGetprices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clicked = populateDummyValues();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (clicked){
            populateDummyValues();
        }
    }

    private boolean populateDummyValues() {
        ArrayList<Entry> pricesHigh = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            pricesHigh.add(new Entry(i, i * 10));
        }

        Log.i(TAG, "data created");
        setLineChartData(pricesHigh);
        return true;
    }

    private void setLineChartData(ArrayList<Entry> pricesHigh) {
        ArrayList<ILineDataSet> dataSets = new ArrayList<>();

        LineDataSet highLineDataSet = new LineDataSet(pricesHigh, " Price (High)");
        highLineDataSet.setDrawCircles(false);
        //highLineDataSet.setCircleRadius(4);
        highLineDataSet.setDrawValues(false);
        highLineDataSet.setLineWidth(3);
        highLineDataSet.setColor(Color.GREEN);
        //highLineDataSet.setCircleColor(Color.GREEN);
        dataSets.add(highLineDataSet);
        Log.i(TAG, "data processing");


        LineData lineData = new LineData(dataSets);
        lineChart.setData(lineData);
        lineChart.invalidate();
        Log.i(TAG, "done");
    }

    private void configureLineChart() {
        Description desc = new Description();
        desc.setText("Stock Price History");
        desc.setTextSize(28);

        lineChart.setDescription(desc);

        /*
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new ValueFormatter() {
            private final DefaultAxisValueFormatter mFormat = new DefaultAxisValueFormatter(1);

        });

         */
    }
}
