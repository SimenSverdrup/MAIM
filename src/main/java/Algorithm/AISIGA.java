package Algorithm;

import AIS.AIS;
import AIS.Antigen;
import AIS.Antibody;
import GUI.GUI;
import Island.IGA;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.HashMap;

public class AISIGA extends Application {

    // private TextField populationSizeInput = new TextField();
    private boolean running = false;
    private GUI gui;
    private AIS ais;
    private ArrayList<AIS> allAIS;
    private boolean radiusPlot = true;

    @Override
    public void start(Stage primaryStage) throws Exception {
        gui = new GUI(primaryStage, this);
    }

    public void run(int iterations,
                    int populationSize,
                    double mutationRate,
                    int numberOfTournaments,
                    String dataSetName,
                    int labelIndex,
                    double trainingTestSplit,
                    double migrationFrequency,
                    int numberOfIslands,
                    double migrationRate,
                    boolean masterIsland,
                    int k,
                    int islandIntegrationCount,
                    int pcaDimensions,
                    boolean radiusPlot,
                    double validationSplit,
                    double radiusMultiplier,
                    boolean plotSolution) {

        if(k > 1){
            this.validateAccuracies(k,
                    iterations,
                    populationSize,
                    mutationRate,
                    numberOfTournaments,
                    dataSetName,
                    labelIndex,
                    migrationFrequency,
                    numberOfIslands,
                    migrationRate,
                    masterIsland,
                    islandIntegrationCount,
                    pcaDimensions,
                    validationSplit,
                    radiusMultiplier);

        }else{
        this.running = true;
        gui.startButton.setDisable(true);
        gui.iterationTextField.setDisable(true);
        gui.setBox.setDisable(true);
        gui.stopButton.setDisable(false);

        DataSet dataSet = new DataSet("./DataSets/" + dataSetName, trainingTestSplit,validationSplit, labelIndex,pcaDimensions);

        IGA iga = new IGA(numberOfIslands, populationSize, iterations, migrationFrequency, migrationRate, masterIsland);
        iga.initialize(dataSet, mutationRate, numberOfTournaments, iterations, radiusMultiplier);

        /*if(iga.hasMaster()){
            this.ais = iga.getMasterIsland().getAis();
        }
        else{
            this.ais = iga.getIsland(0).getAis(); // new
        }*/
                                              // AIS(dataSet.trainingSet,dataSet.featureMap,dataSet.labels,dataSet.antigenMap,populationSize,
                                              // mutationRate,numberOfTournaments,iterations);
        this.allAIS = iga.getAllAIS();
        int islandCount;
        if(masterIsland){
            islandCount = numberOfIslands +1;
        }else{
            islandCount = numberOfIslands;
        }
        gui.createStatisticGraph(iterations,islandCount,masterIsland);

            ArrayList<HashMap<String, ArrayList<Antibody>>>[] antibodyGenerations = new ArrayList[islandCount]; //contains the antibody population for each iteration.
            ArrayList<Double>[] antibodyGenerationAccuracies = new ArrayList[islandCount]; //contains the population accuracies over each iteration.
        int[] bestIterations = new int[islandCount];

        if(plotSolution){

            for(int j=0;j<islandCount;j++){
                antibodyGenerations[j] = new ArrayList<HashMap<String, ArrayList<Antibody>>>();
                antibodyGenerationAccuracies[j] = new ArrayList<>();
            }
        }
            Thread aisThread = new Thread(() -> {
            for (int i = 0; i < iterations; i++) {
                if (!this.getRunning()) {
                    break;
                }
                boolean migrate = iga.migrate();

                if(iga.hasMaster()){
                    iga.migrateMaster(islandIntegrationCount);
                }


                /*double accuracy;
                if(iga.hasMaster()){
                    accuracy = iga.getMasterIsland().getCurrentAccuracy();
                    antibodyGenerationAccuracies[numberOfIslands-1].add(accuracy);
                    antibodyGenerations[numberOfIslands-1].add(AIS.copy(ais.getAntibodyMap()));
                }*/

                if(iga.hasMaster()){
                    double accuracy = iga.getMasterIsland().getCurrentAccuracy();
                    /*int sum=0;
                    for(String label:iga.getMasterIsland().getAis().getLabels()){
                        sum += iga.getMasterIsland().getAis().getAntibodyMap().get(label).size();
                    }

                    System.out.println(sum);*/
                    if(plotSolution) {
                        antibodyGenerationAccuracies[islandCount - 1].add(accuracy);
                        antibodyGenerations[islandCount - 1].add(AIS.copy(iga.getMasterIsland().getAis().getAntibodyMap()));
                    }
                    gui.addIteration(accuracy, migrate,islandCount-1);
                    if (accuracy >= iga.getMasterIsland().getAis().getBestAccuracy() && iga.getMasterIsland().isPopulationChanged()) {
                        bestIterations[islandCount-1] = i;
                        gui.setBestAccuracy(accuracy,islandCount-1);
                        iga.getMasterIsland().getAis().setBestAccuracy(accuracy);
                        iga.getMasterIsland().getAis().setBestIteration(i);
                    }
                }

                for (int j = 0; j < allAIS.size(); j++) {
                    AIS someAIS = allAIS.get(j);
                    double accuracy = AIS.vote(someAIS.getAntigenMap(), someAIS.getAntibodyMap());
                    if(plotSolution) {
                        antibodyGenerationAccuracies[j].add(accuracy);
                        antibodyGenerations[j].add(AIS.copy(someAIS.getAntibodyMap()));
                    }
                    someAIS.setCurrentAccuracy(accuracy);
                    gui.addIteration(accuracy, migrate,j);
                    if (accuracy >= someAIS.getBestAccuracy()) {
                        bestIterations[j] = i;
                        gui.setBestAccuracy(accuracy,j);
                        someAIS.setBestAccuracy(accuracy);
                        someAIS.setBestIteration(i);
                    }

                    someAIS.iterate();
                }
            }

            if(iga.hasMaster()){
                double accuracy = iga.getMasterIsland().getCurrentAccuracy();
                if(plotSolution) {
                    antibodyGenerations[islandCount - 1].add(iga.getMasterIsland().getAis().getAntibodyMap());
                    antibodyGenerationAccuracies[islandCount - 1].add(AIS.vote(iga.getMasterIsland().getAis().getAntigenMap(), iga.getMasterIsland().getAis().getAntibodyMap()));
                }
                gui.addIteration(accuracy, false,islandCount-1);
                if (accuracy >= iga.getMasterIsland().getAis().getBestAccuracy() && iga.getMasterIsland().isPopulationChanged()) {
                    bestIterations[islandCount-1] = antibodyGenerations[islandCount-1].size()-1;
                    gui.setBestAccuracy(accuracy,islandCount-1);
                    iga.getMasterIsland().getAis().setBestAccuracy(accuracy);
                    iga.getMasterIsland().getAis().setBestIteration(antibodyGenerations[islandCount-1].size()-1);
                }
            }
            for (int j = 0; j < allAIS.size(); j++) {
                AIS someAIS = allAIS.get(j);
                double accuracy = AIS.vote(someAIS.getAntigenMap(), someAIS.getAntibodyMap());
                if(plotSolution) {
                    antibodyGenerationAccuracies[j].add(accuracy);
                    antibodyGenerations[j].add(AIS.copy(someAIS.getAntibodyMap()));
                }
                someAIS.setCurrentAccuracy(accuracy);
                gui.addIteration(accuracy, false,j);
                if (accuracy >= someAIS.getBestAccuracy()) {
                    bestIterations[j] = antibodyGenerations[j].size()-1;
                    gui.setBestAccuracy(accuracy,j);
                    someAIS.setBestAccuracy(accuracy);
                    someAIS.setBestIteration(antibodyGenerations[j].size()-1);
                }
            }

            Platform.runLater(() -> {
                gui.startButton.setDisable(false);
                gui.startButton.requestFocus();
                gui.iterationTextField.setDisable(false);
                gui.setBox.setDisable(false);
                gui.stopButton.setDisable(true);
                if(plotSolution) {
                    this.gui.setAntibodyGenerations(antibodyGenerations,bestIterations,antibodyGenerationAccuracies,dataSet.antigenMap,dataSet.testAntigenMap, radiusPlot);
                    if(masterIsland){
                        this.gui.createSolutionGraph(iga.getMasterIsland().getAis().getFeatureMap(), iga.getMasterIsland().getAis().getAntibodyMap());
                    }else{
                        this.gui.createSolutionGraph(iga.getIsland(0).getAis().getFeatureMap(), iga.getIsland(0).getAis().getAntibodyMap());
                    }
                    gui.drawSolution(dataSet.testAntigenMap, antibodyGenerations[islandCount-1].get(bestIterations[islandCount-1]), 0.0, radiusPlot);
                    gui.setBestAccuracyIteration(antibodyGenerationAccuracies[islandCount-1].get(bestIterations[islandCount-1]), bestIterations[islandCount-1]);
                }
            });
        });
        aisThread.start();
        }
    }

    public void validateAccuracies(int k,
                                   int iterations,
                                   int populationSize,
                                   double mutationRate,
                                   int numberOfTournaments,
                                   String dataSetName,
                                   int labelIndex,
                                   double migrationFrequency,
                                   int numberOfIslands,
                                   double migrationRate,
                                   boolean masterIsland,
                                   int islandIntegrationCount,
                                   int pcaDimensions,
                                   double validationSplit,
                                   double radiusMultiplier){


        this.running = true;
        gui.startButton.setDisable(true);
        gui.iterationTextField.setDisable(true);
        gui.stopButton.setDisable(false);

        double[] accuracies = new double[k];
        DataSet dataSet = new DataSet("./DataSets/" + dataSetName, 0.0,0.0, labelIndex,pcaDimensions);
        HashMap<String,ArrayList<Antigen>>[] dataSetSplits = DataSet.splitDataSet(k,dataSet.antigenMap);
        gui.createStatisticGraph(k-1,1,false);

        Thread aisThread = new Thread(() -> {

            double totalBestAccuracy = 0.0;
            for(int j=0; j<accuracies.length;j++){
                if (!this.getRunning()) {
                    break;
                }
            HashMap<String,ArrayList<Antigen>> testSetMap = dataSetSplits[j];
            HashMap<String,ArrayList<Antigen>> trainingSetMap = new HashMap<>();
            HashMap<String,ArrayList<Antigen>> validationSetMap = new HashMap<>();

            ArrayList<Antigen> antigenArrayList = new ArrayList<>();
            for(String label: dataSet.labels){
                trainingSetMap.put(label,new ArrayList<>());
                validationSetMap.put(label,new ArrayList<>());
            }

            for(int n=0; n<dataSetSplits.length;n++){
                if(n == j){
                    continue;
                }
                for(String label: dataSetSplits[n].keySet()){
                    for(Antigen antigen: dataSetSplits[n].get(label)){
                        double p = Math.random();
                        if(p < validationSplit){
                            validationSetMap.get(label).add(antigen);
                        }else{
                            trainingSetMap.get(label).add(antigen);
                            antigenArrayList.add(antigen);
                        }
                    }
                }
            }
            Antigen[] antigens = new Antigen[antigenArrayList.size()];
            antigens = antigenArrayList.toArray(antigens);

            dataSet.setTrainingSet(antigens);
            dataSet.setAntigenMap(trainingSetMap);
            dataSet.setValidationAntigenMap(validationSetMap);

            IGA iga = new IGA(numberOfIslands, populationSize, iterations, migrationFrequency, migrationRate, masterIsland);
            iga.initialize(dataSet, mutationRate, numberOfTournaments, iterations, radiusMultiplier);

            if(iga.hasMaster()){
                this.ais = iga.getMasterIsland().getAis();
            }
            else{
                this.ais = iga.getIsland(0).getAis(); // new
            }

            this.allAIS = iga.getAllAIS();

            ArrayList<HashMap<String, ArrayList<Antibody>>> antibodyGenerations = new ArrayList<>();
                for (int i = 0; i < iterations; i++) {
                    if (!this.getRunning()) {
                        break;
                    }
                    iga.migrate();

                    if(iga.hasMaster()){
                        iga.migrateMaster(islandIntegrationCount);
                    }

                    antibodyGenerations.add(AIS.copy(ais.getAntibodyMap()));
                    double accuracy;
                    if(iga.hasMaster()){
                        accuracy = iga.getMasterIsland().getCurrentAccuracy();
                    }else{
                        accuracy = AIS.vote(ais.getAntigenMap(), ais.getAntibodyMap());
                    }

                    if (accuracy > ais.getBestAccuracy()) {
                        ais.setBestAccuracy(accuracy);
                        ais.setBestIteration(i);
                    }

                    for (int m = 0; m < allAIS.size(); m++) {
                        AIS someAIS = allAIS.get(m);
                        accuracy = AIS.vote(someAIS.getAntigenMap(), someAIS.getAntibodyMap());
                        someAIS.setCurrentAccuracy(accuracy);
                        someAIS.iterate();
                    }
                }

            antibodyGenerations.add(ais.getAntibodyMap());

            HashMap<String, ArrayList<Antibody>> bestGeneration =  antibodyGenerations.get(ais.getBestIteration());

            double accuracy =AIS.vote(testSetMap,bestGeneration);
            gui.addIteration(accuracy, false,0);
            accuracies[j] = accuracy;

            if(accuracy > totalBestAccuracy){
                totalBestAccuracy = accuracy;
                gui.setBestAccuracy(accuracy,0);
            }
            double accuracySum = 0.0;
            int accuracyCount = 0;
            for(double ac: accuracies){
                if(ac != 0.0){
                    accuracySum += ac;
                    accuracyCount++;
                }
            }
            gui.setAverageAccuracy(accuracySum/accuracyCount,0);
        }

        Platform.runLater(() -> {
            gui.startButton.setDisable(false);
            gui.startButton.requestFocus();
            gui.iterationTextField.setDisable(false);
            gui.stopButton.setDisable(true);
        });
        });

        aisThread.start();
    }

    public synchronized boolean getRunning() {
        return running;
    }

    public void stopRunning() {
        running = false;
        gui.startButton.setDisable(false);
        gui.stopButton.setDisable(true);
    }

    @Override
    public void stop() {
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public GUI getGui() {
        return gui;
    }

    public void setGui(GUI gui) {
        this.gui = gui;
    }

    public AIS getAis() {
        return ais;
    }

    public void setAis(AIS ais) {
        this.ais = ais;
    }
}