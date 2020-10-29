package com.example.brakesensor;

import android.os.Bundle;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.View;
import com.example.brakesensor.databinding.ActivityDataLoggingBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DataLoggingActivity extends AppCompatActivity {
    private LineChart lineChart;
    private ActivityDataLoggingBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDataLoggingBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        setSupportActionBar(binding.toolbar);
        lineChart = new LineChart(this);

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Yeet button engaged", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        configureLineChart();
    }

    private void configureLineChart() {
        Description desc = new Description();
        desc.setText("Stock Price History");
        desc.setTextSize(28);

        lineChart.setDescription(desc);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new ValueFormatter() {
            private final SimpleDateFormat mFormat = new SimpleDateFormat("dd MMM", Locale.ENGLISH);

            @Override
            public String getFormattedValue(float value) {
                long millis = (long) value * 1000L;
                return mFormat.format(new Date(millis));
            }
        });
    }
}
