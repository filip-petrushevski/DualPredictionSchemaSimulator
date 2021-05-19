package dual_prediction;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static dual_prediction.PredictingAlgorithm.MOVING_AVERAGE;

public class TwoDimensionalDualPredictionSchemaSimulator {
    // Specified parameters
    private final String fileName = "measurements.csv";
    private final double temperatureThreshold = 10;
    private final double pressureThreshold = temperatureThreshold;
    private final PredictingAlgorithm predictingAlgorithm = MOVING_AVERAGE;

    // Calculated parameters
    private Double temperaturesRootMeanSquareError;
    private Double pressuresRootMeanSquareError;
    private Double fractionOfMeasurementsSent;

    private final List<Double> temperatures = new ArrayList<>();
    private final List<Double> receivedTemperatures = new ArrayList<>();
    private final List<Double> pressures = new ArrayList<>();
    private final List<Double> receivedPressures = new ArrayList<>();
    private Integer numOfMeasurements;

    public static void main(String[] args) throws IOException {
        new TwoDimensionalDualPredictionSchemaSimulator().simulateAndPrintResults();
    }

    private void simulateAndPrintResults() throws IOException {
        readMeasurements();
        calculateMeasurementsReceived();
        calculateRootMeanSquareError();
        printResults();
    }

    private void readMeasurements() throws IOException {
        try (var reader = new BufferedReader(new FileReader(fileName))) {
            reader.readLine(); // skip first line
            String line;
            while ((line = reader.readLine()) != null) {
                String[] lineValues = line.split(",");
                double temperature = Double.parseDouble(lineValues[0]);
                double pressure = Double.parseDouble(lineValues[1]);
                temperatures.add(temperature);
                pressures.add(pressure);
            }
        }
        numOfMeasurements = temperatures.size();
    }

    private void calculateMeasurementsReceived() {
        Integer numOfMeasurementsSent = 1;
        Double lastReceivedTemperatureValue = temperatures.get(0);
        Double secondLastReceivedTemperatureValue = null;
        receivedTemperatures.add(lastReceivedTemperatureValue);
        Double lastReceivedPressureValue = pressures.get(0);
        receivedPressures.add(lastReceivedPressureValue);
        Double secondLastReceivedPressureValue = null;

        for (int i = 1; i < numOfMeasurements; i++) {
            double currTemperatureValue = temperatures.get(i);
            double predictedTemperatureValue = predictValue(lastReceivedTemperatureValue, secondLastReceivedTemperatureValue);
            double currPressureValue = pressures.get(i);
            double predictedPressureValue = predictValue(lastReceivedPressureValue, secondLastReceivedPressureValue);

            if (differenceIsAboveThreshold(currTemperatureValue, predictedTemperatureValue, temperatureThreshold)
                    || differenceIsAboveThreshold(currPressureValue, predictedPressureValue, pressureThreshold)) {
                numOfMeasurementsSent++;
                lastReceivedTemperatureValue = currTemperatureValue;
                lastReceivedPressureValue = currPressureValue;
            } else {
                lastReceivedTemperatureValue = predictedTemperatureValue;
                lastReceivedPressureValue = predictedPressureValue;
            }
            receivedTemperatures.add(lastReceivedTemperatureValue);
            receivedPressures.add(lastReceivedPressureValue);
            secondLastReceivedTemperatureValue = receivedTemperatures.get(i - 1);
            secondLastReceivedPressureValue = receivedPressures.get(i - 1);
        }
        fractionOfMeasurementsSent = ((double) numOfMeasurementsSent) / numOfMeasurements;
    }

    private boolean differenceIsAboveThreshold(Double val1, Double val2, Double threshold) {
        return Math.abs(val1 - val2) > threshold;
    }

    private Double predictValue(Double lastReceivedTemperatureValue, Double secondLastReceivedTemperatureValue) {
        switch (predictingAlgorithm) {
            case LINEAR_PREDICTOR:
                return lastReceivedTemperatureValue;
            case MOVING_AVERAGE:
                if (secondLastReceivedTemperatureValue == null) {
                    return lastReceivedTemperatureValue;
                }
                return (lastReceivedTemperatureValue + secondLastReceivedTemperatureValue) / 2;
            case WEIGHTED_MOVING_AVERAGE:
                if (secondLastReceivedTemperatureValue == null) {
                    return lastReceivedTemperatureValue;
                }
                return (lastReceivedTemperatureValue * 0.75 + secondLastReceivedTemperatureValue * 0.25);
            default:
                throw new RuntimeException("Predicting algorithm not supported");
        }
    }

    private void calculateRootMeanSquareError() {
        temperaturesRootMeanSquareError = Math.pow(
                Stream.iterate(0, i -> i + 1)
                        .limit(numOfMeasurements)
                        .mapToDouble(i -> squaredDifference(receivedTemperatures.get(i), temperatures.get(i)))
                        .average().getAsDouble(),
                0.5);

        pressuresRootMeanSquareError = Math.pow(
                Stream.iterate(0, i -> i + 1)
                        .limit(numOfMeasurements)
                        .mapToDouble(i -> squaredDifference(receivedPressures.get(i), pressures.get(i)))
                        .average().getAsDouble(),
                0.5);
    }

    private double squaredDifference(Double val1, Double val2) {
        return Math.pow(val1 - val2, 2);
    }

    private void printResults() {
        System.out.println("temperaturesRootMeanSquareError = " + temperaturesRootMeanSquareError);
        System.out.println("pressuresRootMeanSquareError = " + pressuresRootMeanSquareError);
        System.out.println("fractionOfMeasurementsSent = " + fractionOfMeasurementsSent);
    }
}
