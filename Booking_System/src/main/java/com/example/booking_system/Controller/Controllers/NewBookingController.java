package com.example.booking_system.Controller.Controllers;

import com.example.booking_system.Controller.System.Managers.SceneManager;
import com.example.booking_system.Controller.System.Managers.SystemManager;
import com.example.booking_system.Controller.System.PubSub.Subject;
import com.example.booking_system.Controller.System.PubSub.Subscriber;
import com.example.booking_system.Controller.ControllerService.ListenerService;
import com.example.booking_system.Controller.ControllerService.ValidationService;
import com.example.booking_system.Model.Models.*;
import com.example.booking_system.Persistence.DAO.BookingDAO_Impl;
import com.example.booking_system.Persistence.DAO.CateringDAO_Impl;
import com.example.booking_system.Persistence.DAO.DepartmentDAO_Impl;
import com.example.booking_system.Persistence.DAO.MeetingRoomDAO_Impl;
import javafx.beans.InvalidationListener;
import javafx.beans.property.Property;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Pair;
import java.net.URL;
import java.sql.Date;
import java.sql.Time;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

public class NewBookingController implements Initializable, Subscriber {
    //region FXML annotation
    @FXML
    private VBox VBoxMain, VBoxMultipleDateOption, VBoxCateringOption;
    @FXML
    private HBox HBoxDateOption;
    @FXML
    private CheckBox checkAdHoc, checkMultiple;
    @FXML
    private DatePicker dpBookingDate;
    @FXML
    private TextField txtDateMultiplier, txtTitle, txtAmountGuest;
    @FXML
    private Label lblErrorTitle, lblErrorGuest;
    @FXML
    private ListView<MeetingRoom> lwMeetingRooms;
    @FXML
    private ListView<LocalDate> lwChosenDates;
    @FXML
    private TextArea txtRoomDetails;
    @FXML
    private ComboBox<Catering> comboCatering;
    @FXML
    private ComboBox<Department> comboDepartment;
    @FXML
    private ComboBox<String> comboStartTime, comboEndTime;
    //endregion
    //region instance variables
    private final MeetingRoomDAO_Impl meetingRoomDAO = new MeetingRoomDAO_Impl();
    private final BookingDAO_Impl bookingDAO = new BookingDAO_Impl();
    private final List<Pair<TextField, Label>> requiredFields = new ArrayList<>();
    private final Map<Property<?>, InvalidationListener> listenerMap = new HashMap<>();
    //endregion
    //region initializer methods
    /**
     * initializes the controller class
     * @param url location
     * @param resourceBundle resources
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        SystemManager.getInstance().subscribe(Subject.User, this);
        SystemManager.getInstance().subscribe(Subject.Institution, this);

        txtRoomDetails.setEditable(false);
        requiredFields.add(new Pair<>(txtTitle, lblErrorTitle));
        requiredFields.add(new Pair<>(txtAmountGuest, lblErrorGuest));

        initListenerMap();

        ListenerService.setupNumericListener(txtDateMultiplier);
        ListenerService.setupNumericListener(txtAmountGuest);

        lwMeetingRooms.getSelectionModel().selectedItemProperty().addListener(observable -> {
            MeetingRoom selectedRoom = lwMeetingRooms.getSelectionModel().getSelectedItem();
            if (selectedRoom != null) {
                selectedRoom.setupDescription();
                txtRoomDetails.setText(selectedRoom.getRoomDescription());
            } else {
                txtRoomDetails.clear();
            }
        });
    }

    /**
     * initializes the listener map for handling property change listeners
     */
    private void initListenerMap() {
        List<Property<?>> properties = List.of(
                checkAdHoc.selectedProperty(),
                checkMultiple.selectedProperty(),
                dpBookingDate.valueProperty(),
                lwChosenDates.itemsProperty(),
                comboStartTime.valueProperty(),
                comboEndTime.valueProperty()
        );

        for (Property<?> property : properties) {
            listenerMap.put(property, null);
        }
    }

    /**
     * initializes display based on the current user's role in system
     */
    private void setupDisplay() {
        setupTimeBoxes();
        setupAdhocListener();
        setupMultipleListener();
        User currentUser = SystemManager.getInstance().getUser();
        if (currentUser.getRole() == Role.STUDENT) {
            disableAdhoc(true);
            disableDate(true);
            checkMultiple.setVisible(false);
            displayMoreDateOption(false);
        } else if (currentUser.getRole() == Role.TEACHER || currentUser.getRole() == Role.ADMIN) {
            disableAdhoc(false);
            disableDate(false);
            checkMultiple.setVisible(true);
            displayMoreDateOption(false);
            setupDateAndTimeListeners(true);
            setupCatering();
        }
    }

    /**
     * initializes time selection boxes based on the institution's opening and closing times
     */
    private void setupTimeBoxes() {
        comboStartTime.getItems().clear();
        comboEndTime.getItems().clear();

        Time openTime = SystemManager.getInstance().getInstitution().getOpenTime();
        Time closeTime = SystemManager.getInstance().getInstitution().getCloseTime();
        int minuteInterval = SystemManager.getInstance().getInstitution().getBookingTimeInterval();

        LocalTime startTime = openTime.toLocalTime();
        LocalTime endTime = closeTime.toLocalTime();

        while (!startTime.isAfter(endTime)) {
            String formattedTime = startTime.toString();
            comboStartTime.getItems().add(formattedTime);
            comboEndTime.getItems().add(formattedTime);
            startTime = startTime.plusMinutes(minuteInterval);
        }
    }

    /**
     * initializes catering based on the user's departments and the institution's catering services
     */
    private void setupCatering() {
        comboCatering.getItems().clear();
        comboDepartment.getItems().clear();

        DepartmentDAO_Impl departmentDAO = new DepartmentDAO_Impl();
        List<Department> departmentList = departmentDAO.readAllFromUser(SystemManager.getInstance().getUser());

        if (departmentList != null) {
            for (Department department : departmentList) {
                comboDepartment.getItems().add(department);
            }
            CateringDAO_Impl cateringDAO = new CateringDAO_Impl();
            List<Catering> cateringList = cateringDAO.readAllFromInstitution(SystemManager.getInstance().getInstitution().getInstitutionID());

            if (cateringList != null) {
                for (Catering catering : cateringList) {
                    comboCatering.getItems().add(catering);
                }
            }
        } else {
            disableCatering(true);
        }
    }

    /**
     * initializes the listener for ad hoc booking selection
     */
    private void setupAdhocListener() {
        ListenerService.setupListener(checkAdHoc.selectedProperty(), listenerMap, observable -> {
            clearTime();
            lwMeetingRooms.getItems().clear();
            if (checkAdHoc.isSelected() && checkMultiple.isSelected()) {
                checkMultiple.setSelected(false);
            }
            if (checkAdHoc.isSelected()) {
                disableDate(true);
                displayMoreDateOption(false);
                disableCatering(true);
                setupDateAndTimeListeners(true);
            } else {
                disableDate(false);
                disableCatering(false);
            }
        });
    }

    /**
     * initializes the listener for multiple dates booking selection
     */
    private void setupMultipleListener() {
        ListenerService.setupListener(checkMultiple.selectedProperty(), listenerMap, observable -> {
            clearTime();
            lwMeetingRooms.getItems().clear();
            if (checkMultiple.isSelected() && checkAdHoc.isSelected()) {
                checkAdHoc.setSelected(false);
            }
            if(checkMultiple.isSelected()) {
                displayMoreDateOption(true);
                setupDateAndTimeListeners(false);
                ListenerService.removeCurrentListener(listenerMap, dpBookingDate.valueProperty());
            } else if (!checkMultiple.isSelected()) {
                lwChosenDates.getItems().clear();
                displayMoreDateOption(false);
                setupDateAndTimeListeners(true);
            }
        });
    }

    /**
     * configures listeners for changes in date and time selections
     * @param singleDate true if single date, false if multiple dates are being considered
     */
    private void setupDateAndTimeListeners(boolean singleDate) {
        if (singleDate) {
            List<Property<?>> properties = List.of(
                    dpBookingDate.valueProperty(),
                    comboStartTime.valueProperty(),
                    comboEndTime.valueProperty()
            );
            ListenerService.setupListeners(properties, listenerMap, observable -> checkDateAndTime(true));
            ListenerService.removeCurrentListener(listenerMap, lwChosenDates.itemsProperty());
            ListenerService.removeCurrentListener(listenerMap, txtDateMultiplier.textProperty());
        } else {
            List<Property<?>> properties = List.of(
                    lwChosenDates.itemsProperty(),
                    comboStartTime.valueProperty(),
                    comboEndTime.valueProperty()
            );
            ListenerService.setupListeners(properties, listenerMap, observable -> checkDateAndTime(false));
            ListenerService.removeCurrentListener(listenerMap, dpBookingDate.valueProperty());
        }
    }
    //endregion
    //region handler methods
    /**
     * handles adding one or more selected date to list of chosen dates
     */
    @FXML
    void onPlusClick() {
        LocalDate chosenDate = dpBookingDate.getValue();
        if (chosenDate != null) {
            if (!txtDateMultiplier.getText().isEmpty()) {
                if (ValidationService.validateStringIsInt(txtDateMultiplier.getText())) {
                    int multiplier = Integer.parseInt(txtDateMultiplier.getText());
                    for (int i = 0; i < multiplier; i++) {
                        if (i == 0) {
                            lwChosenDates.getItems().add(chosenDate);
                        } else {
                            lwChosenDates.getItems().add(chosenDate.plusWeeks(i));
                        }
                        sortDateList();
                        txtDateMultiplier.clear();
                    }
                }
            } else {
                lwChosenDates.getItems().add(chosenDate);
                sortDateList();
            }
        }
    }

    /**
     * handles removing selected date from list of chosen dates
     */
    @FXML
    void onMinusClick() {
        LocalDate chosenDate = lwChosenDates.getSelectionModel().getSelectedItem();
        if (chosenDate != null) {
            lwChosenDates.getItems().remove(chosenDate);
        }
    }

    /**
     * handles resetting view and closing of window
     */
    @FXML
    void onCancelClick() {
        clearAll();
        SceneManager.closeScene(VBoxMain.getScene());
    }

    /**
     * handles the creating of a new booking in the system
     */
    @FXML
    void onBookClick() {
        if (validateBooking()) {
            String bookingTitle = txtTitle.getText();
            User currentUser = SystemManager.getInstance().getUser();
            int userID = currentUser.getUserID();
            String responsible = currentUser.getFirstName();
            int roomID = lwMeetingRooms.getSelectionModel().getSelectedItem().getRoomID();
            boolean adHoc = dpBookingDate.getValue().equals(LocalDate.now());
            LocalTime start = LocalTime.parse(comboStartTime.getValue());
            LocalTime end = LocalTime.parse(comboEndTime.getValue());
            Time startTime = Time.valueOf(start);
            Time endTime = Time.valueOf(end);
            Duration duration = Duration.between(start, end);
            int durationMinutes = (int) duration.toMinutes();
            int attendance = Integer.parseInt(txtAmountGuest.getText());

            boolean result = false;
            LocalDate date = adHoc ? LocalDate.now() : dpBookingDate.getValue();
            Date sqlDate = Date.valueOf(date);

            if (checkMultiple.isSelected()) {
                List<LocalDate> dates = lwChosenDates.getItems();
                if (!dates.isEmpty()) {
                    int menuID = validateCatering() ? comboCatering.getSelectionModel().getSelectedItem().getMenuID() : 0;
                    int departmentID = validateCatering() ? comboDepartment.getSelectionModel().getSelectedItem().getDepartmentID() : 0;

                    for (LocalDate chosenDate : dates) {
                        sqlDate = Date.valueOf(chosenDate);
                        result = bookingDAO.add(new Booking(bookingTitle, userID, responsible, roomID, adHoc, sqlDate, startTime, endTime, durationMinutes, attendance, menuID, departmentID));
                    }
                }
            } else {
                int menuID = validateCatering() ? comboCatering.getSelectionModel().getSelectedItem().getMenuID() : 0;
                int departmentID = validateCatering() ? comboDepartment.getSelectionModel().getSelectedItem().getDepartmentID() : 0;

                result = bookingDAO.add(new Booking(bookingTitle, userID, responsible, roomID, adHoc, sqlDate, startTime, endTime, durationMinutes, attendance, menuID, departmentID));
            }

            if (result) {
                clearAll();
                SystemManager.getInstance().notifySubscribers(Subject.Institution);
            }
        }
    }
    //endregion
    //region view methods
    /**
     * sets the ad hoc booking option to either enabled or disabled
     * @param disable true if disable, false if enable
     */
    private void disableAdhoc(boolean disable) {
        checkAdHoc.setSelected(disable);
        checkAdHoc.setDisable(disable);
    }

    /**
     * disables or enables the date selection field, if enabled sets its value to the current date
     * @param disable true if disable, false if enable
     */
    private void disableDate(boolean disable) {
        if (disable) {
            dpBookingDate.setValue(LocalDate.now());
        }
        dpBookingDate.setDisable(disable);
    }

    /**
     * displays or hides additional date options for multiple-date bookings
     * @param display true if displayed, false if hide
     */
    private void displayMoreDateOption(boolean display) {
        HBoxDateOption.setVisible(display);
        VBoxMultipleDateOption.setVisible(display);
    }

    /**
     * disables catering options
     * @param disable true if disable, false if enable
     */
    private void disableCatering(boolean disable) {
        comboCatering.setValue(null);
        comboDepartment.setValue(null);
        VBoxCateringOption.setVisible(!disable);
    }

    /**
     * clears view and set it to default state
     */
    private void clearAll() {
        txtDateMultiplier.clear();
        lwChosenDates.getItems().clear();
        clearTime();
        lwMeetingRooms.getItems().clear();
        txtRoomDetails.clear();
        txtTitle.clear();
        txtAmountGuest.clear();
        lwMeetingRooms.getItems().clear();
        txtRoomDetails.clear();
        comboCatering.getSelectionModel().clearSelection();
        comboDepartment.getSelectionModel().clearSelection();
        User currentUser = SystemManager.getInstance().getUser();
        if (currentUser.getRole() == Role.TEACHER || currentUser.getRole() == Role.ADMIN) {
            checkAdHoc.setSelected(false);
            checkMultiple.setSelected(false);
            dpBookingDate.setValue(null);
            setupDateAndTimeListeners(true);
        }
    }

    /**
     * clears time boxes
     */
    private void clearTime() {
        comboStartTime.getSelectionModel().clearSelection();
        comboEndTime.getSelectionModel().clearSelection();
    }
    //endregion
    //region validation methods
    /**
     * validates if start and end times is set
     * @return true if set, false if not
     */
    private boolean validateTime() {
        return comboStartTime.getSelectionModel().getSelectedItem() != null
                && comboEndTime.getSelectionModel().getSelectedItem() != null;
    }

    /**
     * validates if catering is set
     * @return true if set, false if not
     */
    private boolean validateCatering() {
        return comboCatering.getSelectionModel().getSelectedItem() != null
                && comboDepartment.getSelectionModel().getSelectedItem() != null;
    }

    /**
     * validates if all necessary data is in order to create a booking
     * @return true if validation pass, false if not
     */
    private boolean validateBooking() {
        return ValidationService.validateFieldsEntered(requiredFields)
                && lwMeetingRooms.getSelectionModel().getSelectedItem() != null
                && ValidationService.validFieldLength(txtTitle, 30, lblErrorTitle)
                && ValidationService.validateStringIsInt(txtAmountGuest.getText())
                && ValidationService.validFieldLength(txtTitle, 30, lblErrorTitle);
    }
    //endregion
    //region functional method
    /**
     * checks for available meeting rooms based on selected date(s) and time range
     * @param singleDate true if a single date is selected, false if multiple selected
     */
    private void checkDateAndTime(boolean singleDate) {
        if (validateTime()) {

            LocalTime start = LocalTime.parse(comboStartTime.getValue());
            LocalTime end = LocalTime.parse(comboEndTime.getValue());

            if (!end.isAfter(start)) {
                lwMeetingRooms.getItems().clear();
                return;
            }

            int institutionID = SystemManager.getInstance().getInstitution().getInstitutionID();
            Time startTime = Time.valueOf(start);
            Time endTime = Time.valueOf(end);

            List<MeetingRoom> meetingRoomList = null;

            if (singleDate && dpBookingDate.getValue() != null) {
                Date choosenDate = Date.valueOf(dpBookingDate.getValue());
                meetingRoomList = meetingRoomDAO.readAllAvailableOnDate(institutionID, choosenDate, startTime, endTime);
            } else if (!singleDate && lwChosenDates.getItems() != null) {
                List<LocalDate> chosenDates = lwChosenDates.getItems();
                List<Date> searchDates = new ArrayList<>();
                for (LocalDate date : chosenDates) {
                    searchDates.add(Date.valueOf(date));
                }
                meetingRoomList = meetingRoomDAO.readAllAvailableInPeriod(institutionID, searchDates, startTime, endTime);
            }

            if (meetingRoomList != null) {
                lwMeetingRooms.getItems().setAll(meetingRoomList);
            }
        }
    }
    //endregion
    //region helper methods
    /**
     * sorts the list of chosen dates in ascending order
     */
    private void sortDateList() {
        ObservableList<LocalDate> dates = lwChosenDates.getItems();
        FXCollections.sort(dates);
    }
    //endregion
    //region subscriber method
    /**
     * updates the view to only show and allow for user privileges
     */
    @Override
    public void onUpdate() {
        if (SystemManager.getInstance().getUser() != null) {
            setupDisplay();
        }
    }
    //endregion
}