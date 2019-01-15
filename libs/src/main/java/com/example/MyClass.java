package com.example;

import de.greenrobot.daogenerator.DaoGenerator;
import de.greenrobot.daogenerator.Entity;
import de.greenrobot.daogenerator.Property;
import de.greenrobot.daogenerator.Schema;
import de.greenrobot.daogenerator.ToMany;

public class MyClass {

    public static void main(String[] args) throws Exception {
        Schema schema = new Schema(1, "com.ryannm.android.autoanki.Dao");
        addConfiguration(schema);
        addBlackList(schema);
        addAnkiNote(schema);
        //addEvernoteTag(schema);

        new DaoGenerator().generateAll(schema, System.getProperty("user.dir").replace("\\","/")+"/app/src/main/java");
    }

    private static void addCustomerOrder(Schema schema) {
        Entity customer = schema.addEntity("Customer");
        customer.addIdProperty();
        customer.addStringProperty("name").notNull();

        Entity order = schema.addEntity("Order");
        order.setTableName("ORDERS"); // "ORDER" is a reserved keyword
        order.addIdProperty();
        Property orderDate = order.addDateProperty("date").getProperty();
        Property customerId = order.addLongProperty("customerId").notNull().getProperty();
        order.addToOne(customer, customerId);

        ToMany customerToOrders = customer.addToMany(order, customerId);
        customerToOrders.setName("orders");
        customerToOrders.orderAsc(orderDate);
    }

    private static void addConfiguration(Schema schema) {
        Entity configuration = schema.addEntity("Configuration");
        configuration.addIdProperty();
        configuration.addStringProperty("name");
        configuration.addStringProperty("tagsToFetch");
        configuration.addStringProperty("tagsToSave");
        configuration.addLongProperty("modelId");
        configuration.addLongProperty("deckId");
        configuration.addStringProperty("fields");
        configuration.addStringProperty("fieldKeywords");
        configuration.setHasKeepSections(true);
    }

    private static void addBlackList(Schema schema) {
        Entity blackList = schema.addEntity("BlackList");
        blackList.addStringProperty("id").primaryKey();
        blackList.addStringProperty("noteTitle");
        blackList.addIntProperty("cardsAdded");
        blackList.addStringProperty("notebook");
    }

    private static void addAnkiNote(Schema schema) {
        Entity ankiNote = schema.addEntity("AnkiNote");
        ankiNote.addIdProperty();
        ankiNote.addLongProperty("configurationId");
        ankiNote.addStringProperty("fields");
        ankiNote.addLongProperty("deckId");
        ankiNote.addStringProperty("tags");
        ankiNote.addStringProperty("notebookTitle");
        ankiNote.addStringProperty("noteTitle");
    }

}
