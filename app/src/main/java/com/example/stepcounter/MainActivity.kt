package com.example.stepcounter

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.example.stepcounter.ui.theme.StepCounterTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity(), SensorEventListener {

    private var sensorManager: SensorManager? = null

    private var running = false
    private var totalSteps = 0f
    private var previousTotalSteps = 0f

    private var stepsTaken by mutableStateOf("0")
    private var goalSteps by mutableStateOf("10000")
    private var isGoalSet by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPrefs = getSharedPreferences("myPrefs", Context.MODE_PRIVATE)

        setContent {
            StepCounterTheme {
                if (!isGoalSet) {
                    GoalInputScreen { goal ->
                        goalSteps = goal
                        isGoalSet = true
                        with(sharedPrefs.edit()) {
                            putBoolean("isGoalSet", true)
                            putString("goal", goal)
                            apply()
                        }
                    }

                } else {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color(0xFFA1A1A1)
                    ) {
                        StepCounterLayout(steps = stepsTaken, goal = goalSteps, resetSteps = ::resetSteps, newGoal = ::newGoal)
                    }
                }
            }
        }
        loadData()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACTIVITY_RECOGNITION), 0)
        }
    }

    override fun onResume() {
        super.onResume()
        running = true
        val stepSensor : Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        if (stepSensor == null) {
            Toast.makeText(this, "No sensor detected on this device", Toast.LENGTH_SHORT).show()
        } else {
            sensorManager?.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (running) {
            totalSteps = event!!.values[0]
            val currentSteps = totalSteps.toInt() - previousTotalSteps.toInt()
            stepsTaken = "$currentSteps"
        }
    }

    private fun resetSteps() {
        previousTotalSteps = totalSteps
        stepsTaken = "0"
        saveData()
    }

    private fun newGoal() {
        isGoalSet = false
    }

    private fun saveData() {
        val sharedPreferences = getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putFloat("key1", previousTotalSteps)
        editor.apply()
    }

    private fun loadData() {
        val sharedPreferences = getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        val savedNumber = sharedPreferences.getFloat("key1", 0f)
        Log.d("MainActivity", "$savedNumber")
        previousTotalSteps = savedNumber
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed
    }
}
@Composable
fun GoalInputScreen(onGoalSet: (String) -> Unit) {
    var goal by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.DarkGray)
        ,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = goal,
            onValueChange = { goal = it },
            label = { Text("Enter your step goal") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier.padding(16.dp)
        )
        Button(
            onClick = {
                if (goal.isNotEmpty()) {
                    onGoalSet(goal)
                    coroutineScope.launch {
                        // Ensure that the UI is updated after setting the goal
                        delay(100)
                    }

                }
            },
            enabled = goal.isNotEmpty()
        ) {
            Text("Set Goal")
        }
    }
}

@Composable
fun StepCounterLayout(steps: String, goal: String, resetSteps: () -> Unit, newGoal: () -> Unit) {
    val progress = steps.toFloat() / goal.toFloat()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .border(3.dp, Color.Black, shape = RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp)) // Clip background color to rounded corners
                .background(Color.Gray) // Set background color for the Box and Buttons

        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(3.dp)
                ) {
                    CircularProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .size(320.dp)
                            .padding(12.dp),
                        trackColor = Color.DarkGray,
                        color = Color.Red
                    )
                    Text(text = "$steps/$goal")
                }
                Row(
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Button(onClick = resetSteps, modifier = Modifier.padding(8.dp)) {
                        Text("Reset")
                    }
                    Button(onClick = newGoal, modifier = Modifier.padding(8.dp)) {
                        Text("Change goal")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StepCounterLayoutPreview() {
    StepCounterTheme {
        StepCounterLayout("0", "10000", resetSteps = {}, newGoal = {})
    }
}
