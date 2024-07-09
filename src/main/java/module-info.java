module assign2.assign2 {
    requires javafx.controls;
    requires javafx.fxml;


    opens assign2.assign2 to javafx.fxml;
    exports assign2.assign2;
}