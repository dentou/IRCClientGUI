package com.github.dentou.controller;

import com.github.dentou.model.chat.Channel;
import com.github.dentou.utils.FXUtils;
import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class ChannelDialogController extends ChatDialogController {

    @FXML
    private Button leaveButton;

    @FXML
    private ListView<String> channelListView;

    @FXML
    private Label blockedLabel;
    @FXML
    private Label channelNameLabel;
    @FXML
    private Label channelTopicLabel;

    private Channel channel;

    @Override
    public synchronized void setBlocked(boolean blocked) {
        super.setBlocked(blocked);
        blockedLabel.setText(blocked ? "You are not a member of this channel" : "");

    }

    @FXML
    @Override
    protected void initialize() {
        super.initialize();
        blockedLabel.setText("");
    }

    @Override
    public void disableAll() {
        super.disableAll();
        FXUtils.setDisabled(true, leaveButton);
    }

    @Override
    public void enableAll() {
        super.enableAll();
        FXUtils.setDisabled(true, fileButton); // File button is always disabled for channel
        FXUtils.setDisabled(false, leaveButton);
    }

    public synchronized void setChannel(Channel channel, ObservableSet<String> memberSet) {
        this.channel = channel;

        channelListView.getItems().addAll(memberSet);

        memberSet.addListener((SetChangeListener.Change<? extends String> c) -> {
            if (c.wasAdded()) {
                channelListView.getItems().add(c.getElementAdded());
            }
            if (c.wasRemoved()) {
                channelListView.getItems().remove(c.getElementRemoved());
            }
        });

        // todo
        setChannelListViewOption();

        channelNameLabel.setText(channel.getName());
        channelTopicLabel.setText(channel.getTopic());
    }

    private void setChannelListViewOption() {
        channelListView.setCellFactory(lv -> {
            ListCell<String> cell = new ListCell<>();

            ContextMenu contextMenu = new ContextMenu();

            MenuItem kickItem = new MenuItem();
            kickItem.setStyle("-fx-text-fill: white;");
            kickItem.textProperty().bind(Bindings.format("Kick \"%s\"", cell.itemProperty()));
            kickItem.setOnAction(event -> {
                // Send KICK request to server.
                String nick = cell.getItem();
                if (nick.charAt(0) == '@' || nick.charAt(0) == '+') {
                    nick = nick.substring(1);
                }
                getMainApp().getIrcClient().sendToServer("KICK", " ",
                        channel.getName(), " ",
                        nick);
            });

            contextMenu.getItems().add(kickItem);

            cell.textProperty().bind(cell.itemProperty());

            cell.emptyProperty().addListener((obs, wasEmpty, isNowEmpty) -> {
                if (isNowEmpty) {
                    cell.setContextMenu(null);
                } else {
                    cell.setContextMenu(contextMenu);
                }
            });
            return cell ;
        });
    }


    @FXML
    private void onLeave() {
        getMainApp().getIrcClient().sendToServer("PART", " ", channel.getName());
        disableAll();
        this.setBlocked(true);
    }

}
