package project.api;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.layout.HBox;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.layout.VBox;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Alert;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.scene.control.ProgressBar;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.GridPane;
import javafx.collections.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import javafx.scene.control.TextArea;
import javafx.geometry.Pos;
import javafx.scene.text.Font;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import java.util.LinkedHashSet;
import javafx.scene.layout.Priority;
import javafx.geometry.Insets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

/**
 * Represents an API App.
 */
public class ApiApp extends Application {

    /**
     * Class representing the response of the FindWork API.
     *
     */
    private static class FindWorkResponse {
        int count;
        FindWorkResult[] results;
    }

    /**
     * Class representing the result in a response from the FindWork API.
     *
     */
    private static class FindWorkResult {
        String role;
        @SerializedName("company_name")
        String companyName;
        String location;
        @SerializedName("date_posted")
        String datePosted;
        String[] keywords;
    }

    /**
     * Class representing an Open Library Search API document.
     */
    private static class OpenLibraryDoc {
        String title;
        @SerializedName("author_name")
        String[] authorName;
        @SerializedName("cover_i")
        String coverID; // used to retrieve the image.
    }

    /**
     * Class representing an Open Library Search API result.
     */
    private static class OpenLibraryResult {
        int numFound;
        OpenLibraryDoc[] docs;
    }


    /** HTTP client. */
    public static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_2)           // uses HTTP protocol version 2 where possible
        .followRedirects(HttpClient.Redirect.NORMAL)  // always redirects, except from HTTPS to HTTP
        .build();                                     // builds and returns a HttpClient object

    /** Google {@code Gson} object for parsing JSON-formatted strings. */
    public static Gson GSON = new GsonBuilder()
        .setPrettyPrinting()                          // enable nice output when printing
        .create();                                    // builds and returns a Gson object

    private Stage stage;
    private Scene scene;
    private VBox root;
    private HBox searchBar;
    private HBox messageBar;
    private HBox statusBar;
    private GridPane grid;
    private Label[] jobInfo;
    private ImageView[] imgViewers;
    private Label search;
    private TextField locationQueryField;
    private TextField searchQueryField;
    private Button getImages;
    private Label instructions;
    private ProgressBar progressBar;
    private Label progressBarText;
    private String uri;
    private String apiKey;
    private int count;
    private ObservableList<Image> images;
    private static final String CONFIG_PATH = "resources/config.properties";
    private static final String DEFAULT_IMG = "file:resources/White_background.png";
    private static final String IMG_NOT_AVAILABLE = "file:resources/img_not_available.png";
    private static final String FINDWORK_API = "https://findwork.dev/api/jobs/";
    private static final String OPENLIBRARY_API = "https://openlibrary.org/search.json";
    private static final String OPENLIBRARY_IMG = "https://covers.openlibrary.org/b/id/";

    /**
     * Constructs a {@code ApiApp} object}.
     */
    public ApiApp() {
        this.stage = null;
        this.scene = null;
        this.root = new VBox();
        searchBar = new HBox(1);
        messageBar = new HBox(1);
        statusBar = new HBox(1);
        grid = new GridPane();
        imgViewers = new ImageView[15];
        jobInfo = new Label[5];
        search = new Label("Search:");
        locationQueryField = new TextField();
        searchQueryField = new TextField();
        getImages = new Button ("Get Results");
        instructions = new Label
        ("Type in a job title and optionally, the preferred location, and click the button");
        progressBar = new ProgressBar(0.0);
        progressBarText = new Label ("Images provided by OpenLibrary Search API");
        uri = "";
        images = FXCollections.observableArrayList();
        try (FileInputStream configFileStream = new FileInputStream(CONFIG_PATH)) {
            Properties config = new Properties();
            config.load(configFileStream);
            apiKey = config.getProperty("findworkapi.apikey");
        } catch (IOException ioe) {
            alertError(ioe, FINDWORK_API);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void init() {
        root.getChildren().addAll(searchBar, messageBar, grid, statusBar);
        searchBar.getChildren().addAll(search, searchQueryField,
            locationQueryField, getImages);
        messageBar.getChildren().add(instructions);
        statusBar.getChildren().addAll(progressBar, progressBarText);
        locationQueryField.setPromptText("Location:");
        searchQueryField.setPromptText("Job Title: e.g Engineer");
        searchBar.setAlignment(Pos.CENTER_LEFT);
        messageBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(searchQueryField, Priority.ALWAYS);
        Insets inset = new Insets(3.0);
        HBox.setMargin(search, inset);
        HBox.setMargin(locationQueryField, inset);
        HBox.setMargin(searchQueryField, inset);
        HBox.setMargin(getImages, inset);
        HBox.setMargin(instructions, inset);
        HBox.setMargin(progressBar, inset);
        HBox.setMargin(progressBarText, inset);
        Image defaultImage = new Image(DEFAULT_IMG, 100, 100, false, true);
        int count = 0;
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 4; j++) {
                if (j == 0) {
                    jobInfo[i] = new Label();
                    jobInfo[i].setPrefWidth(300);
                    grid.add(jobInfo[i], j, i);
                } else {
                    imgViewers[count] = new ImageView();
                    imgViewers[count].setImage(defaultImage);
                    grid.add(imgViewers[count], j , i);
                    count++;
                }
            }
        }
        progressBar.setPrefWidth(300);
        getImages.setOnAction((ActionEvent e) -> {
            runOnNewThread(() -> {
                retrieveImages();
            });
        });
    }

    /** {@inheritDoc} */
    @Override
    public void start(Stage stage) {
        this.stage = stage;
        this.scene = new Scene(this.root);
        this.stage.setOnCloseRequest(event -> Platform.exit());
        this.stage.setTitle("Api-App!");
        this.stage.setScene(this.scene);
        this.stage.sizeToScene();
        this.stage.show();
        Platform.runLater(() -> this.stage.setResizable(false));
    }

    /** {@inheritDoc} */
    @Override
    public void stop() {
    }

    /**
     * Creates and immediately starts a new daemon thread that executes
     * {@code target.run()}. This method, which may be called from any thread,
     * will return immediately its the caller.
     * @param target the object whose {@code run} method is invoked when this
     *               thread is started
     */
    public static void runOnNewThread(Runnable target) {
        Thread t = new Thread(target);
        t.setDaemon(true);
        t.start();
    }

    /**
     * Helper method that creates the URI used by the HttpRequest.
     *
     * @return a {@code URI}
     */
    public String createURI() {
        String location = URLEncoder.encode(locationQueryField.getText(), StandardCharsets.UTF_8);
        String search = URLEncoder.encode(searchQueryField.getText(), StandardCharsets.UTF_8);
        String relevance = URLEncoder.encode("relevance", StandardCharsets.UTF_8);
        String query =
            String.format("?location=%s&search=%s&sort_by=%s",
            location, search, relevance);
        String uri = FINDWORK_API + query;
        return uri;
    }

    /**
     * Helper method that creates the URI used by the HttpRequest.
     *
     * @param search a String containing what is to be searched in the request.
     * @return a {@code URI}
     *
     */
    public String createURI2(String search) {
        String q = URLEncoder.encode(search, StandardCharsets.UTF_8);
        String rating = URLEncoder.encode("rating", StandardCharsets.UTF_8);
        String query = String.format("?q=%s&sort=%s", q, rating);
        String uri = OPENLIBRARY_API + query;
        return uri;
    }

    /**
     * Helper method that creates and returns the HttpRequest.
     * Used for the FindWork API.
     *
     * @param uri the link to be used for the {@code HttpRequest}
     * @return {@code HttpRequest}
     *
     */
    public HttpRequest createRequest(String uri) {
        HttpRequest request = HttpRequest.newBuilder() // build request
            .uri(URI.create(uri))
            .header("Authorization", "Token " + apiKey)
            .build();
        return request;
    }

    /**
     * Helper method that creates and returns the HttpRequest.
     * Used for the OpenLibrary API.
     *
     * @param uri the link to be used for the {@code HttpRequest}
     * @return {@code HttpRequest}
     *
     */
    public HttpRequest createRequest2(String uri) {
        HttpRequest request = HttpRequest.newBuilder() // build request
            .uri(URI.create(uri))
            .build();
        return request;
    }


    /**
     * Hepler method that takes in FindWorkResult array and returns
     * a FindWorkResult array with only the first 5 items that contain at least
     * 1 key word and also update Labels with text.
     *
     * @param findWorkResults
     * @return {@code FindWorkResult} array
     */
    public FindWorkResult[] firstFive(FindWorkResult[] findWorkResults) {
        FindWorkResult[] firstFive = new FindWorkResult[5];
        count = 0; //change the instance variable
        for (int i = 0; i < findWorkResults.length; i++) {
            if (count == 5) {
                break;
            }
            if (findWorkResults[i].keywords.length != 0) { //if they have at least 1 keyword
                firstFive[count] = findWorkResults[i];
                count++;
            }
        }
        Platform.runLater(() -> {
            for (int i = 0; i < count; i++) {
                String text = "Company name: " + firstFive[i].companyName
                    + "\nJob title: " + firstFive[i].role
                    + "\nLocation: " + firstFive[i].location
                    + "\nDate Posted: " + firstFive[i].datePosted;
                jobInfo[i].setText(text);
            }
        });
        return firstFive;
    }

    /**
     * Helper method that creates uris, sends requests, and retrieve responses from FindWork API
     * and then parses the responses. Then, using the information from the responses, create urls
     * and then with those urls, create images and put images into the ImageViews.
     *
     * @param firstFive an array of FindWorkResult
     * @param openLibraryResults an array of OpenLibraryResult
     * @param openLibraryDocs an array of OpenLibraryDoc
     */

    public void helper(FindWorkResult[] firstFive, OpenLibraryResult[] openLibraryResults,
        OpenLibraryDoc[] openLibraryDocs) throws IOException, InterruptedException {
        int count2 = 0;
        for (int i = 0; i < count; i++) {
//for each job result we found (the 5), take the first keyword in the keywords array of the result
//and create a new uri, build a new request, and retrieve a response.
            String uriBooks = createURI2
                (firstFive[i].keywords[0] + "+" + searchQueryField.getText());
            HttpRequest requestBooks = createRequest2(uriBooks);
            HttpResponse<String> responseBooks = HTTP_CLIENT.send
                (requestBooks, BodyHandlers.ofString());
            if (responseBooks.statusCode() != 200) { //ensurethe request is okay
                throw new IOException(responseBooks.toString());
            }
            openLibraryResults[i] = GSON.fromJson(responseBooks.body(), OpenLibraryResult.class);
            if (openLibraryResults[i].numFound < 3) {
                count2 = count2 + (3 - openLibraryResults[i].numFound);
            }
            int bookCount = 0; //tracks the amount of books added for each result.
//for each of these 5 results, iterate through numFound books
            for (int j = 0; j < openLibraryResults[i].numFound; j++) {
//check if book has coverID. if yes, update img in imgView. If no, check next book of same keyword.
                if (bookCount == 3) { //if we have 3 books added, end loop
                    break;
                }
                if (openLibraryResults[i].docs[j].coverID != null) {
                    String imgUrl = OPENLIBRARY_IMG
                        + openLibraryResults[i].docs[j].coverID + "-L.jpg";
                    images.add(count2, new Image(imgUrl, 100, 100, false, true));
                    count2++; //add everytime we add an image to the images list
                    bookCount++;
                    progressBar.setProgress(count2 / 15.0); //update progressBar
                } else {
                    if (j == openLibraryResults[i].numFound - 1) {
                        images.add(count2, new Image(IMG_NOT_AVAILABLE, 100, 100, false, true));
                        count2++; //add everytime we add an image to the images list
                        bookCount++;
                        progressBar.setProgress(count2 / 15.0); //update progressBar
                    }
                }
            }
        }
        for (int k = 0; k < 15; k++) {
            imgViewers[k].setImage(images.get(k));
        }
    }

    /**
     * Creates an {@code HttpRequest} to the iTunes search API using the
     * {@code HttpClient} and stores the response and parses it using
     * {@code Gson} and updates the images in the {@code ImageView}'s.
     *
     */
    public void retrieveImages() {
        try {
            //disable the Get Images button and change instructions, on the JavaFX App Thread
            Platform.runLater(() -> startOfGetImages());
            uri = createURI();
            HttpRequest request = createRequest(uri);
            HttpResponse<String> response = HTTP_CLIENT // send request&receive response as a String
                .send(request, BodyHandlers.ofString());
            if (response.statusCode() != 200) {  // ensure the request is okay
                throw new IOException(response.toString());
            }
            FindWorkResponse findWorkResponse = GSON  // parse the JSON-formatted string using GSON
                .fromJson(response.body(), FindWorkResponse.class);
            FindWorkResult[] findWorkResults = findWorkResponse.results;

            if (findWorkResults.length == 0) { // if there were 0 results for the job search
                throw new IllegalArgumentException("0 results were found");
            }
            FindWorkResult[] firstFive = firstFive(findWorkResults);
            OpenLibraryResult[] openLibraryResults = new OpenLibraryResult[count];
            OpenLibraryDoc[] openLibraryDocs = new OpenLibraryDoc[15];
            helper(firstFive, openLibraryResults, openLibraryDocs);
            Platform.runLater(() -> endOfGetImages());
        } catch (IOException | InterruptedException | IllegalArgumentException e) {
            Platform.runLater(() -> {
                getImages.setDisable(false);
                instructions.setText("Last attempt to get images failed..");
                alertError(e, uri);
            });
        }
    }

    /**
     * Helper method that deals with some scene graph
     * occurrences that should happen when getImages is pressed.
     *
     */
    public void startOfGetImages() {
        getImages.setDisable(true);
        instructions.setText("Getting search results and images of corresponding books...");
    }

    /**
     * Helper method that deals with some scene graph
     * occurrences that should happen at the end of when getImages is pressed.
     *
     */
    public void endOfGetImages() {
        getImages.setDisable(false);
        instructions.setText("Job search results below provided by FindWork API");
        progressBar.setProgress(1);
    }

    /**
     * Show a modal error alert based on {@code cause}.
     * @param cause a {@link java.lang.Throwable Throwable} that caused the alert
     * @param uri a String that represents the uri that was used when this error occurred.
     *
     */
    public static void alertError(Throwable cause, String uri) {
        TextArea text = new TextArea("URI: " + uri  + "\n\nException: " + cause.toString());
        text.setEditable(false);
        Alert alert = new Alert(AlertType.ERROR);
        alert.getDialogPane().setContent(text);
        alert.setResizable(true);
        alert.showAndWait();
    } // alertError

} // ApiApp
