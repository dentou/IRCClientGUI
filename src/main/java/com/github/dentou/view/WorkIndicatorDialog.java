package com.github.dentou.view;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import static javafx.scene.paint.Color.TRANSPARENT;


/**
 * Public domain. Use as you like. No warranties.
 * P = Input parameter type. Given to the closure as parameter
 * R = Return type
 *
 */
public class WorkIndicatorDialog<P, R> { 
    private Task animationWorker;
    private Task<R> taskWorker;

    private final int width = 240;
    private final int height = 180;

    private final ProgressIndicator progressIndicator = new ProgressIndicator(ProgressIndicator.INDETERMINATE_PROGRESS);
    private final Stage dialog = new Stage(StageStyle.TRANSPARENT);
    private final Label label = new Label();
    private final Group root = new Group();
    private final Scene scene = new Scene(root, Color.TRANSPARENT);
    private final BorderPane mainPane = new BorderPane();
    private final VBox vbox = new VBox();

    /** Placing a listener on this list allows to get notified BY the result when the task has finished. */
    public ObservableList<R> resultNotificationList=FXCollections.observableArrayList();

    public R resultValue;

    /**
     *
     */
    public WorkIndicatorDialog(Window owner, String label) {
        dialog.setWidth(width);
        dialog.setHeight(height);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(owner);
        dialog.setResizable(false);

        this.label.setText(label);

        this.label.setStyle("-fx-text-fill: white; -fx-font-size: 14px");
        dialog.setX(owner.getX() + owner.getWidth()/2 - dialog.getWidth()/2);
        dialog.setY(owner.getY() + owner.getHeight()/2 - dialog.getHeight()/2);

    }

    /**
     *
     */
    public void addTaskEndNotification(Consumer<R> c) {
        resultNotificationList.addListener((ListChangeListener<? super R>) n -> {
            resultNotificationList.clear();
            c.accept(resultValue);
        });
    }

    /**
     *
     */
    public void execute(P parameter, Function<P, R> func) {
        setupDialog();
        setupAnimationThread();
        setupWorkerThread(parameter, func);
    }

    /**
     *
     */
    private void setupDialog() {

        mainPane.setStyle("-fx-background-radius: 20px; -fx-background-color: rgb(0, 0, 0, 0.75);");

        //progressIndicator.setStyle("-fx-progress-color: red;");
        root.getChildren().add(mainPane);
        vbox.setSpacing(5);
        vbox.setAlignment(Pos.CENTER);
        vbox.setMinSize(width, height);
        vbox.getChildren().addAll(label,progressIndicator);
        mainPane.setTop(vbox);
        dialog.setScene(scene);

        dialog.setOnHiding(event -> { /* Gets notified when task ended, but BEFORE
                     result value is attributed. Using the observable list above is
                     recommended. */ });

        dialog.show();
    }

    /**
     *
     */
    private void setupAnimationThread() {

        animationWorker = new Task() {
            @Override
            protected Object call() throws Exception {
                /*
                This is activated when we have a "progressing" indicator.
                This thread is used to advance progress every XXX milliseconds.
                In case of an INDETERMINATE_PROGRESS indicator, it's not of use.
                for (int i=1;i<=100;i++) {
                    Thread.sleep(500);
                    updateMessage();
                    updateProgress(i,100);
                }
                */
                return true;
            }
        };

        progressIndicator.setProgress(0);
        progressIndicator.progressProperty().unbind();
        progressIndicator.progressProperty().bind(animationWorker.progressProperty());

        animationWorker.messageProperty().addListener((observable, oldValue, newValue) -> {
            // Do something when the animation value ticker has changed
        });

        new Thread(animationWorker).start();
    }

    /**
     *
     */
    private void setupWorkerThread(P parameter, Function<P, R> func) {

        taskWorker = new Task<R>() {
            @Override
            public R call() {
                return func.apply(parameter);
            }
        };

        EventHandler<WorkerStateEvent> eh = event -> {
            animationWorker.cancel(true);
            progressIndicator.progressProperty().unbind();
            dialog.close();
            try {
                resultValue = taskWorker.get();
                resultNotificationList.add(resultValue);
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        taskWorker.setOnSucceeded(eh);
        taskWorker.setOnFailed(eh);

        new Thread(taskWorker).start();
    }

    /**
     * For those that like beans :)
     */
    public R getResultValue() {
        return resultValue;
    }
}
