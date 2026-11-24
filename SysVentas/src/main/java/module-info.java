module pe.edu.upeu.sysventas {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires static lombok;
    requires jakarta.validation;

    requires java.sql;
    requires com.h2database;
    requires java.naming;
    requires org.slf4j;
    requires com.zaxxer.hikari;
    requires javafx.graphics;
    requires java.prefs;
    requires org.jsoup;
    requires java.desktop;
    requires escpos.coffee;
    requires com.github.librepdf.openpdf;
    requires org.apache.pdfbox;


    opens pe.edu.upeu.sysventas to javafx.fxml;
    opens pe.edu.upeu.sysventas.controller to javafx.fxml;
    opens pe.edu.upeu.sysventas.model;
    exports pe.edu.upeu.sysventas;
}