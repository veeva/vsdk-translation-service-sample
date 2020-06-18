package com.veeva.vault.custom.triggers;

import com.veeva.vault.sdk.api.core.*;
import com.veeva.vault.sdk.api.data.*;
import com.veeva.vault.sdk.api.i18n.TranslationService;
import com.veeva.vault.sdk.api.i18n.TranslationsReadRequest;
import com.veeva.vault.sdk.api.query.QueryResponse;
import com.veeva.vault.sdk.api.query.QueryResult;
import com.veeva.vault.sdk.api.query.QueryService;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Map;


/*
 * This sample displays a translated pop up error message(translated to French) on a bicycle order object record for the following cases:
 * 1. When the Order Quantity(order_quantity__c) exceeds the current stock for that bicycle model, it will display the order_quantity_exceeds_stock__c message.
 * 2. When the model is out of stock(quantity is 0), it will display the out_of_stock__c error message.
 * 3. When the model cannot be found, it will display the product_does_not_exist__c order error message.
 *
 * It assumes that a custom validation message group and messages has already been created.
 * Please follow the setup directions in the wiki to set this up.
 *
 * More documentation regarding translation messages can be found here:
 * http://vaulthelp2.vod309.com/wordpress/admin-user-help/viewing-vault-java-sdk-solutions/#message_catalog
 * http://vaulthelp2.vod309.com/wordpress/admin-user-help/using-the-message-catalog/
 *
 */

/**
 * This class annotation (@RecordTriggerInfo) indicates that this class is a record trigger.
 * It specifies the object that this trigger will run on(bicycle_order__c), the events it will run on(BEFORE_INSERT, BEFORE_UPDATE) and the order(1st).
 */
@RecordTriggerInfo(object = "bicycle_order__c", events = {RecordEvent.BEFORE_INSERT, RecordEvent.BEFORE_UPDATE}, order = TriggerOrder.NUMBER_1)
public class vSDKTranslationServiceSampleOrderFieldValueValidation implements RecordTrigger {

    public void execute(RecordTriggerContext recordTriggerContext) {

        // Constant value for the out of stock quantity
        final BigDecimal OUT_OF_STOCK_QUANTITY = new BigDecimal(0);

        recordTriggerContext.getRecordChanges().forEach(recordChange -> {

            // Get the bicycle model name, manufacture name and quantity entered by the user.
            String productName = recordChange.getNew().getValue("product__c", ValueType.STRING);
            String bicycleManufacturerName = recordChange.getNew().getValue("bicycle_manufacturer__c", ValueType.STRING);
            BigDecimal orderQuantity = recordChange.getNew().getValue("order_quantity__c", ValueType.NUMBER);

            /*
             * Get an instance of the Query Service used to find if the bicycle model exists and it's quantity.
             * The Query service is used to execute VQL queries to retrieve documents or object record.
             * VQL is a Structured Query Language(SQL) like querying language used to access document or object records.
             * More information about VQL Queries can be found here: https://developer.veevavault.com/vql/#introduction-to-vault-queries
             * Query Service Java Doc can be found here: https://repo.veevavault.com/javadoc/vault-sdk-api/20.1.0/docs/api/index.html
             */
            QueryService queryService = ServiceLocator.locate(QueryService.class);

            // Build our query string
            String queryString = "SELECT id, quantity__c FROM bicycle_model__c WHERE name__v= '"
                    + productName + "' AND bicycle_manufacturer__cr.name__v= '" + bicycleManufacturerName + "'";
            // Running our query and getting the response.
            QueryResponse queryResponse = queryService.query(queryString);

            /*
             * Get an instance of the Translation Service used to fetch translated message groups and messages.
             * Translation Service Java Doc can be found here:
             * https://repo.veevavault.com/javadoc/vault-sdk-api/20.1.2/docs/api/com/veeva/vault/sdk/api/i18n/package-summary.html
             */
            TranslationService translationService = ServiceLocator.locate(TranslationService.class);

            // Build the request to fetch our validation message group.
            TranslationsReadRequest readRequest = translationService.newTranslationsReadRequestBuilder()
                    .withMessageGroup("validation_message__c")
                    .build();

            // Read all messages in the group
            Map<String, String> allValidationMessages = translationService.readTranslations(readRequest)
                    .getTranslations();

            // Use an iterator to iterate over our results(should be only 1)
            Iterator<QueryResult> queryResults = queryResponse.streamResults().iterator();

            // If there are no results, then the product does not exist.
            if(queryResponse.getResultCount() == 0) {
                // Get the translated message for product not found
                String errorTranslation = allValidationMessages.get("product_does_not_exist__c");

                // Display the translated error message to the user
                recordChange.setError("PRODUCT_ERROR", errorTranslation);
            }

            while(queryResults.hasNext()) {

                // Get a queryResult
                QueryResult queryResult = queryResults.next();

                // Get id and quantity of the bicycle model.
                String id = queryResult.getValue("id", ValueType.STRING);
                BigDecimal productQuantity = queryResult.getValue("quantity__c", ValueType.NUMBER);

                if (id == null) {
                    // Get the translated message for product not found
                    String errorTranslation = allValidationMessages.get("product_does_not_exist__c");

                    // Display the translated error message to the user
                    recordChange.setError("PRODUCT_ERROR", errorTranslation);

                } else if (productQuantity.compareTo(OUT_OF_STOCK_QUANTITY) == 0) { // compareTo returns 0 if the product quantity equals OUT_OF_STOCK_QUANTITY
                    // Get the translated message for out of stock
                    String errorTranslation = allValidationMessages.get("out_of_stock__c");

                    // Display the translated error message to the user
                    recordChange.setError("OUT_OF_STOCK_ERROR", errorTranslation);

                } else if (productQuantity.compareTo(orderQuantity) < 0) { // compareTo returns negative if the product quantity is less than order quantity
                    // Get the translated message for order quantity exceeding stock
                    String errorTranslation = allValidationMessages.get("order_quantity_exceeds_stock__c");

                    // Display the translated error message to the user
                    recordChange.setError("ORDER_QUANTITY_ERROR", errorTranslation);
                }
            }
        });
    }
}