package com.github.gcnyin.sample;

import com.gluonhq.attach.display.DisplayService;
import com.gluonhq.attach.storage.StorageService;
import com.gluonhq.attach.util.Platform;
import com.gluonhq.attach.util.Services;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.control.FloatingActionButton;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import com.gluonhq.charm.glisten.visual.Swatch;
import javafx.event.ActionEvent;
import javafx.geometry.Dimension2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import vproxybase.util.Logger;
import vproxyx.WebSocksProxyAgent;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main extends MobileApplication {
    private final FloatingActionButton fab = new FloatingActionButton();
    private WebSocksProxyAgent webSocksProxyAgent;
    private Console console;
    private PrintStream ps;
    private static final TextArea textArea = new TextArea();

    public static class Console extends OutputStream {

        private final TextArea output;
        private PrintWriter printWriter;

        public Console(TextArea ta) {
            this.output = ta;
            try {
                Path path = Services.get(StorageService.class)
                        .flatMap(StorageService::getPrivateStorage)
                        .orElseThrow(() -> new FileNotFoundException("Could not access private storage.")).toPath();
                File file = Files.createFile(Paths.get(path.toString(), "vpwsagent.log")).toFile();
                printWriter = new PrintWriter(new FileWriter(file));
            } catch (IOException e) {
                ta.appendText(e.toString());
            }
        }

        @Override
        public void write(int i) throws IOException {
            javafx.application.Platform.runLater(() ->
            {
                String text = String.valueOf((char) i);
                output.appendText(text);
                if (printWriter != null) {
                    printWriter.write(text);
                }
            });
        }
    }

    @Override
    public void init() {
        addViewFactory(HOME_VIEW, () -> {

            fab.setText(MaterialDesignIcon.CACHED.text);
            fab.setOnAction(this::startWsAgent);

            ImageView imageView = new ImageView(new Image(Main.class.getResourceAsStream("openduke.png")));
            imageView.setFitHeight(200);
            imageView.setPreserveRatio(true);

            Label label = new Label("Hello, Vproxy WsAgent!");

            console = new Console(textArea);
            ps = new PrintStream(console, true);

            Logger.out = ps;

            VBox root = new VBox(20, imageView, label, textArea);
            root.setAlignment(Pos.CENTER);

            View view = new View(root) {
                @Override
                protected void updateAppBar(AppBar appBar) {
                    appBar.setTitleText("Vproxy WsAgent");
                }
            };

            fab.showOn(view);

            return view;
        });
    }

    @Override
    public void postInit(Scene scene) {
        Swatch.DEEP_PURPLE.assignTo(scene);
        scene.getStylesheets().add(Main.class.getResource("styles.css").toExternalForm());

        if (Platform.isDesktop()) {
            Dimension2D dimension2D = DisplayService.create()
                    .map(DisplayService::getDefaultDimensions)
                    .orElse(new Dimension2D(640, 480));
            scene.getWindow().setWidth(dimension2D.getWidth());
            scene.getWindow().setHeight(dimension2D.getHeight());
        }
    }

    private void startWsAgent(ActionEvent actionEvent) {
        webSocksProxyAgent = new WebSocksProxyAgent();
        try {
            String s = "agent.listen 1081\n" +
                    "agent.gateway on\n" +
                    "agent.pool 4\n" +
                    "\n" +
                    "proxy.server.auth luobotou:2ojfiuh32\n" +
                    "\n" +
                    "proxy.server.hc off\n" +
                    "\n" +
                    "agent.cert.verify on\n" +
                    "\n" +
                    "proxy.server.list.start\n" +
                    "websockss:kcp://45.76.215.74:443\n" +
                    "proxy.server.list.end\n" +
                    "\n" +
                    "proxy.domain.list.start\n" +
                    "google.com\n" +
                    "github.io\n" +
                    "proxy.domain.list.end\n" +
                    "\n" +
                    "no-proxy.domain.list.start\n" +
                    "no-proxy.domain.list.end\n";
            InputStream inputStream = new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
//            InputStream configInputStream = Main.class.getResourceAsStream("vpws-agent.conf");
            webSocksProxyAgent.launch(inputStream);
            fab.setText(MaterialDesignIcon.DONE.text);
            fab.setOnAction(this::stopWsAgent);
        } catch (Exception e) {
            e.printStackTrace(ps);
            fab.setText(MaterialDesignIcon.ERROR.text);
            fab.setOnAction(this::startWsAgent);
        }
    }

    private void stopWsAgent(ActionEvent actionEvent) {
        webSocksProxyAgent.stop();
        fab.setText(MaterialDesignIcon.CACHED.text);
        fab.setOnAction(this::startWsAgent);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
