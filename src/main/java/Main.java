import AIS.AIS;
import AIS.Antigen;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class Main extends Application {


    public static void main(String[] args) {
        launch(args);
    }
    @Override
    public void start(Stage primaryStage) throws Exception {
        BorderPane borderPane = new BorderPane();
        Scene scene = new Scene(borderPane, 1200, 800);
        primaryStage.setScene(scene);
        primaryStage.show();

        DataSet dataSet = new DataSet("./DataSets/iris.data",0.1);

        AIS ais = new AIS(dataSet.trainingSet,dataSet.antigenMap,100, 0.1, 5);
        for(int i=0;i<100;i++){
            ais.iterate();
        }
        ais.vote(dataSet.testSet);
        /*for(Antigen antigen:dataSet.antigens){
            System.out.println(antigen);
        }*/
    }
}
