package places;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Widgetset;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Page;
import com.vaadin.server.StreamResource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.*;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.vaadin.addon.leaflet.LLayerGroup;
import org.vaadin.addon.leaflet.LMap;
import org.vaadin.addon.leaflet.LMarker;
import org.vaadin.addon.leaflet.LOpenStreetMapLayer;
import org.vaadin.easyuploads.MultiFileUpload;
import org.vaadin.maddon.MBeanFieldGroup;
import org.vaadin.maddon.button.ConfirmButton;
import org.vaadin.maddon.button.MButton;
import org.vaadin.maddon.fields.MTable;
import org.vaadin.maddon.fields.MTextField;
import org.vaadin.maddon.form.AbstractForm;
import org.vaadin.maddon.layouts.MFormLayout;
import org.vaadin.maddon.layouts.MHorizontalLayout;
import org.vaadin.maddon.layouts.MVerticalLayout;
import org.vaadin.spring.UIScope;
import org.vaadin.spring.VaadinComponent;
import org.vaadin.spring.VaadinUI;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventBusListenerMethod;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Optional;


@Theme("valo")
@Widgetset("AppWidgetset")
@VaadinUI
public class PlacesUI extends UI {

    @Autowired
    private PlacesTableView placesTableView;

    @Autowired
    private PlacesMapView placesMapView;

    @Override
    protected void init(VaadinRequest request) {

        Page.getCurrent().setTitle("'Bootiful' Vaadin Places"); // use Spring's i18n

        TabSheet sheet = new TabSheet();
        sheet.setSizeFull();
        sheet.addComponents( this.placesMapView,  this.placesTableView);
        this.setContent(sheet);
    }
}

@UIScope
@VaadinComponent
class PlacesTableView extends MVerticalLayout implements InitializingBean {

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private PlaceForm form;

    @Autowired
    private EventBus eventBus;

    private MTable<Place> list = new MTable<>(Place.class)
            .withProperties("name", "street", "city", "state", "zip", "category")
            .withColumnHeaders("name", "street", "city", "state", "zip", "category")
            .withFullWidth();

    private Button edit = new MButton(
            FontAwesome.PENCIL_SQUARE_O,
            clickEvent -> {
                form.setEntity(this.placeRepository.findOne(list.getValue().getId()));
                form.openInModalPopup().setHeight("90%");
            });

    private Button delete = new ConfirmButton(FontAwesome.TRASH_O,
            "Are you sure you want to delete the entry?",
            clickEvent -> {
                Place place = list.getValue();
                placeRepository.delete(place);
                list.setValue(null);
                listEntities();
                eventBus.publish(this, new PlaceModifiedEvent(place));
            });

    @PostConstruct
    protected void init() {
        setCaption("Tabular View");
        addComponents(new MVerticalLayout(new MHorizontalLayout(edit, delete), list).expand(list));
        listEntities();
        list.addMValueChangeListener(e -> adjustActionButtonState());
    }

    private void adjustActionButtonState() {
        boolean hasSelection = list.getValue() != null;
        edit.setEnabled(hasSelection);
        delete.setEnabled(hasSelection);
    }

    private void listEntities() {
        list.setBeans(placeRepository.findAll());
        adjustActionButtonState();
    }

    @EventBusListenerMethod
    protected void onPlaceModifiedEvent(PlaceModifiedEvent event) {
        listEntities();
        UI.getCurrent().getWindows().forEach(UI.getCurrent()::removeWindow);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        eventBus.subscribe(this);
    }
}


class PlaceModifiedEvent {
    private final Place place;

    public Place getPlace() {
        return place;
    }

    public PlaceModifiedEvent(Place place) {
        this.place = place;
    }
}

@UIScope
@VaadinComponent
class PlacesMapView extends LMap implements InitializingBean {

    private LLayerGroup layerGroup = new LLayerGroup();

    @Autowired
    private EventBus eventBus;

    @Autowired
    private PlaceForm placeForm;

    @Autowired
    private PlaceRepository placeRepository;

    @Override
    public void afterPropertiesSet() throws Exception {
        this.eventBus.subscribe(this);
        this.refreshPlaces(null);
    }

    @EventBusListenerMethod
    public void refreshPlaces(PlaceModifiedEvent o) {
        this.layerGroup.removeAllComponents();
        this.placeRepository.findAll().forEach(p -> {
            LMarker marker = new LMarker(p.getLatitude(), p.getLongitude());
            marker.addClickListener(leafletClickEvent -> {
                Place place = this.placeRepository.findOne(p.getId());
                this.placeForm.setEntity(place);
                this.placeForm.openInModalPopup().setHeight("90%");
            });
            this.layerGroup.addComponent(marker);
        });
        zoomToContent();
    }

    public PlacesMapView() {
        setCaption("Map view");
        addLayer(new LOpenStreetMapLayer());
        addLayer(this.layerGroup);
        addClickListener(e -> Notification.show("you clicked on " +
                e.getPoint().getLon() + ", " + e.getPoint().getLat()));
    }
}


@UIScope
@VaadinComponent
class PlaceForm extends AbstractForm<Place> {

    private MTextField name = new MTextField("Name");
    private MTextField category = new MTextField("Category");
    private MTextField about = new MTextField("About");
    private MTextField city = new MTextField("City");
    private MTextField state = new MTextField("State");
    private MTextField zip = new MTextField("Postal Code");
    private MultiFileUpload upload = new MultiFileUpload() {

        @Override
        protected void handleFile(File file, String fileName, String mimeType, long length) {
            try {
                gridFsTemplate.store(new FileInputStream(file), place.getId());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            displayImageIfAvailable(place.getId());
        }
    };
    private MTextField country = new MTextField("Country");

    private Image image;

    @Autowired
    private EventBus eventBus;

    @Autowired
    private GridFsTemplate gridFsTemplate;

    @Autowired
    private PlaceRepository placeRepository;

    private Place place;

    private MFormLayout layout;

    @Override
    public MBeanFieldGroup<Place> setEntity(Place entity) {
        MBeanFieldGroup<Place> placeMBeanFieldGroup = super.setEntity(entity);
        this.place = entity;
        return placeMBeanFieldGroup;
    }

    protected void displayImageIfAvailable(String imageId) {
        if (image != null) {
            layout.removeComponent(image);
        }
        Optional.ofNullable(this.gridFsTemplate.getResource(imageId))
                .ifPresent(gfr -> {
                    image = new Image("Image", new StreamResource(
                            (StreamResource.StreamSource) () -> {
                                try {
                                    return gridFsTemplate.getResource(imageId).getInputStream();
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }, "photo" + this.place.getId() + System.currentTimeMillis() + ".jpg"));
                    image.setWidth(400, Unit.PIXELS);
                    layout.addComponent(image);
                });
    }

    public PlaceForm() {

        this.setSizeUndefined();
        this.setEagarValidation(true);
        this.setSavedHandler(place -> {
            this.placeRepository.save(place);
            this.eventBus.publish(this, new PlaceModifiedEvent(place));
        });

        this.getResetButton().setVisible(false);
    }


    @Override
    protected Component createContent() {

        layout = new MFormLayout(
                this.name,
                this.category,
                this.about,
                this.city,
                this.state,
                this.zip,
                this.country,
                this.upload
        ).withWidth("");

        this.displayImageIfAvailable(this.place.getId());

        return new MVerticalLayout(layout, getToolbar()).withWidth("");
    }
}
