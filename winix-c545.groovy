import groovy.transform.Field

// To Do:
//  2. Ensure every line of code is tracable
//  4. check fields from winix for known fields vs added fields
//  6.  separate out key from domain name interface

// ignore the following keys
// [utcDatetime, utcTimestamp, S07]

@Field final static String DRIVER_NAME = "Winix C545"
@Field final static String DRIVER_VERSION = "0.1.0"

@Field final static String TRACE = "TRACE"
@Field final static String DEBUG = "DEBUG"
@Field final static String INFO = "INFO"
@Field final static String WARN = "WARN"
@Field final static String ERROR = "ERROR"

@Field final static List < String > LOG_OPTIONS = [
    "TRACE",
    "DEBUG",
    "INFO",
    "WARN",
    "ERROR"
]

@Field final static List < String > POLLING_FREQUENCY = [
    "1 Minute", 
    "5 Minutes", 
    "10 Minutes", 
    "15 Minutes", 
    "30 Minutes", 
    "1 Hour", 
    "3 Hours"
]

@Field final static short FILTER_REPLACEMENT_IN_HOURS = 6480

@Field final static short STATUS_DELAY_IN_MS = 500

@Field final static short STATUS_DELAY_IN_MS_MAX = 10000

@Field final static Map DEVICE = [
    switch: [
        attr: [
            dataType: "enum",
            deviceToWinixLookup: [
                off: "0",
                on: "1"
            ],
            winixStatusKey: "A02"
        ]
    ],
    mode: [
        attr: [
            dataType: "enum",
            deviceToWinixLookup: [
                auto: "01",
                manual: "02"
            ],
            winixStatusKey: "A03"
        ],
        cmd: [
            name: "setMode"
        ]
    ],
    airflow: [
        attr: [
            dataType: "enum",
            deviceToWinixLookup: [
                low: "01",
                medium: "02",
                high: "03",
                turbo: "05",
                sleep: "06"
            ],
            winixStatusKey: "A04",
        ],
        cmd: [
            name: "setAirflow"
        ]
    ],
    plasmawave: [
        attr: [
            dataType: "enum",
            deviceToWinixLookup: [
                off: "0",
                on: "1"
            ],
            winixStatusKey: "A07",
        ],
        cmd: [
            name: "setPlasmawave"
        ]
    ],
    filterUsage: [
        attr: [
            dataType: "integer",
            winixStatusKey: "A21",
            unitOfMeasure: "hours"
        ]
    ],
    ambientLight: [
        attr: [
            dataType: "integer",
            winixStatusKey: "S14"
        ]
    ],
    airQualityValue: [
        attr: [
            dataType: "integer",
            winixStatusKey: "S08"
        ]
    ],
    airQualityIndicator: [
        attr: [
            dataType: "enum",
            deviceToWinixLookup: [
                good: "01",
                fair: "02",
                poor: "03"
            ],
            winixStatusKey: "A05"
        ]
    ],
    apiNo: [
        attr: [
            dataType: "string",
            winixStatusKey: "apiNo"
        ]
    ],
    apiGroup: [
        attr: [
            dataType: "string",
            winixStatusKey: "apiGroup"
        ]
    ],
    winixStateCreationTimeMS: [
        attr: [
            dataType: "number",
            winixStatusKey: "creationTime",
            unitOfMeasure: "ms that have elapsed since January 1, 1970"
        ]
    ],
    deviceGroup: [
        attr: [
            dataType: "string",
            winixStatusKey: "deviceGroup"
        ]
    ],
    modelId: [
        attr: [
            dataType: "string",
            winixStatusKey: "modelId"
        ]
    ],
    rssi: [
        attr: [
            dataType: "integer",
            winixStatusKey: "rssi",
            unitOfMeasure: "dBm"
        ]
    ]
]

// device accepted command.  does not guarantee actual success
@Field final static Map WINIX_RESPONSE_CONTROL_SUCCESS = [
    statusCode: 200,
    headers: [
        resultCode: "S100",
        resultMessage: "control success"
    ],
    body: [:]
]

// device id is not valid
@Field final static Map WINIX_RESPONSE_BAD_DEVICE_ID = [
    statusCode: 200,
    headers: [
        resultCode: "F300",
        resultMessage: "parameter(s) not valid : device id"
    ],
    body: [:]
]

// device is disconnected from wifi
@Field final static Map WINIX_RESPONSE_DEVICE_NOT_CONNECTED = [
    statusCode: 200,
    headers: [
        resultCode: "F400",
        resultMessage: "device not connected"
    ],
    body: [:]
]

// device is either disconnected or not checked in for a while
@Field final static Map WINIX_RESPONSE_NO_DATA = [
    statusCode: 200,
    headers: [
        resultCode: "S100",
        resultMessage: "no data"
    ],
    body: [:]
]

metadata {
    definition(
        name: DRIVER_NAME,
        namespace: "marshalltech81",
        author: "Marshall Thompson",
        importURL: "https://raw.githubusercontent.com/marshalltech81/hubitat-winix-c545/initial/winix-c545.driver.groovy"
    ) {

        capability "Refresh"
        capability "Polling"
        capability "Switch"

        attribute "winixStateCreationDateString", "string"
        // attribute "errorBadDeviceId", "boolean"
        // attribute "errorNotConnected", "boolean"
        // attribute "errorNoData", "boolean"
        attribute "warnReplaceFilter", "boolean"
        attribute "correlationID", "string"

        DEVICE.each {
            String deviceStateName, Map value ->
            String dataType = value.attr.dataType
            List < String > deviceToWinixLookup = value.attr.deviceToWinixLookup?.keySet() as List < String >

            // log.trace("Creating Device Attribute (name) (dataType) (deviceToWinixLookup if enum): ($deviceStateName) ($dataType) ($deviceToWinixLookup)")
            attribute deviceStateName, dataType, deviceToWinixLookup

            // if there is a command associated with a given state
            if (value.cmd) {
                command value.cmd.name, [
                    [name: deviceStateName, type: value.attr.dataType.toUpperCase(), constraints: deviceToWinixLookup]
                ]
            }
        }
    }
    preferences {
        input "logging", "enum", title: "Log Level", required: true, defaultValue: INFO, options: LOG_OPTIONS
        input "pollFreq", "enum", title: "Polling Frequency", required: false, options: POLLING_FREQUENCY
        input "filterReplacementInHours", "number", title: "Filter Replacement (hours)", required: true, defaultValue: FILTER_REPLACEMENT_IN_HOURS, range: 0..FILTER_REPLACEMENT_IN_HOURS
        input "statusDelayInMS", "number", title: "Status Delay (milliseconds)", required: true, defaultValue: STATUS_DELAY_IN_MS, range: 0..STATUS_DELAY_IN_MS_MAX
        input "winixDeviceKey", "password", title: "Winix Device Key", required: true
        input "onIfOff", "bool", title: "Turn Device On if Off", required: true, defaultValue: true
    }
}

@Field final String correlationID = UUID.randomUUID().toString()

@Field Map CachedStateLookupFor = [:]

/**
 * Run when the hubitat driver is installed.
 */

@groovy.transform.CompileStatic
@groovy.transform.TypeChecked
void installed() {
    String prependLogMsg = "installed() -- "

    log("$prependLogMsg BEGIN", DEBUG)

    log("$prependLogMsg Device installing.", INFO)
    
    sync()

    log("$prependLogMsg END", DEBUG)
}

/**
 * Run when the hubitat driver is updated (when Save Preferences is clicked).
 */

@groovy.transform.CompileStatic
@groovy.transform.TypeChecked
void updated() {
    String prependLogMsg = "updated() -- "

    log("$prependLogMsg BEGIN", DEBUG)

    log("$prependLogMsg Device updating", INFO)

    sync()

    log("$prependLogMsg END", DEBUG)
}

/**
 * Run when hubitat driver is initialized (first loads).
 */

@groovy.transform.CompileStatic
@groovy.transform.TypeChecked
void initialize() {
    String prependLogMsg = "initialize() -- "

    log("$prependLogMsg BEGIN", DEBUG)

    log("$prependLogMsg Device initializing", INFO)
    
    sync()

    log("$prependLogMsg END", DEBUG)
}

/**
 * Run when a request is issued to refresh the device state to hubitat
 */

@groovy.transform.CompileStatic
@groovy.transform.TypeChecked
void refresh() {
    sync()
}

/**
 * Run when a request is issued to refresh the device state to hubitat
 */

@groovy.transform.CompileStatic
@groovy.transform.TypeChecked
void poll() {
    sync()
}

/**
 * Run when a request is issued to refresh the device state to hubitat
 * 
 * @param doWeSendEvents indicates if we send events to the device changing the device's state.
 *  we may not want to change the devices state immediately like in the case of if we need to 
 *  perform a sync to get the state of the physical device before performing some action
 */

void sync(final boolean doWeSendEvents = true) {
    String prependLogMsg = "sync($doWeSendEvents) --"
    
    log("$prependLogMsg BEGIN", DEBUG)
    
    unschedule()
    log("$prependLogMsg Unscheduling previously scheduled polling jobs", TRACE)
    
    if (settings.pollFreq) {
        this."runEvery${settings.pollFreq - ' '}"("sync")
    }
    
    log("$prependLogMsg Syncing device state to Hubitat", INFO)

    // send a dummy command so that the air purifier will sync with the cloud
    sendWinixCommand("hc", "ping")
    
    if (doWeSendEvents) {
        sendEvents()
    }
    
    log("$prependLogMsg END", DEBUG)
}

/**
 * Run when a request is issued to turn on the device
 */

@groovy.transform.CompileStatic
@groovy.transform.TypeChecked
void on() {
    String prependLogMsg = "on() -- "

    log("$prependLogMsg BEGIN", DEBUG)

    log("$prependLogMsg Turning device on", INFO)

    sendWinixCommand("switch", "on")
    
    sendEvents()

    log("$prependLogMsg END", DEBUG)
}

/**
 * Gets cached state value in case the state has changed in between runs without being synced to the device yet
 */

def getCachedStateValue(String deviceStateName) {
    String prependLogMsg = "getCachedStateValue($deviceStateName) --"
    
    log("$prependLogMsg BEGIN", DEBUG)
    
    if (CachedStateLookupFor.containsKey(deviceStateName)) {
        if (CachedStateLookupFor[deviceStateName].containsKey("value")) {
            return CachedStateLookupFor[deviceStateName].value
        } else {
            throw new Exception("TODO")
        }
    } else {
        return device.currentValue(deviceStateName)
    }
    
    log("$prependLogMsg END", DEBUG)
}

/**
 * Turn device on if off
 */

void onIfOff() {
    String prependLogMsg = "onIfOff() --"
                                    
    log("$prependLogMsg BEGIN", DEBUG)
        
    log("$prependLogMsg Settings onIfOff (${settings.onIfOff})", TRACE)
    
    if (settings.onIfOff) {
        log("$prependLogMsg Preparing to turn the device on if off", TRACE)
        
        sync(false)

        String deviceSwitchState = getCachedStateValue("switch")
        log("$prependLogMsg Device Switch State ($deviceSwitchState)", TRACE)
        
        if (deviceSwitchState == "off") {
            on()
        }
    }
    
    log("$prependLogMsg END", DEBUG)
}

/**
 * Run when a request is issued to turn off the device
 */

@groovy.transform.CompileStatic
@groovy.transform.TypeChecked
void off() {
    String prependLogMsg = "off() -- "

    log("$prependLogMsg BEGIN", DEBUG)

    log("$prependLogMsg Turning device off", INFO)

    sendWinixCommand("switch", "off")
    
    sendEvents()

    log("$prependLogMsg END", DEBUG)
}

/**
 * Run when a request is issued to set the airflow of the device
 *
 * @param airflow low, medium, high, turbo, sleep
 */

@groovy.transform.CompileStatic
@groovy.transform.TypeChecked
void setAirflow(String airflow) {
    String prependLogMsg = "setAirflow($airflow) -- "

    log("$prependLogMsg BEGIN", DEBUG)

    log("$prependLogMsg Setting Airflow to $airflow", INFO)

    onIfOff() // ensure device is on before sending the command
    sendWinixCommand("airflow", airflow)

    sendEvents()
    
    log("$prependLogMsg END", DEBUG)
}

/**
 * Send cached events to device.
 * 
 * This function is designed to solve the problems where sometimes there are multiple "sync" operations required to process a request and we only want to 
 * collect one set of events to send to the device.  
 */

void sendEvents() {
    String prependLogMsg = "sendEvents() -- "

    log("$prependLogMsg BEGIN", DEBUG)
    
    CachedStateLookupFor.each {
        String deviceStateName, Map value ->
    
        def deviceStateValueCurrent = device.currentValue(deviceStateName)
        def deviceStateValueDesired = value.value
        def deviceStateValueUnits   = value.unit
    
        // deviceStateValueCurrent & deviceStateValueDesired do not seem to respect typing of variables so casting them to String for comparison
        //  otherwise variables of the same value but different types will not be symantically equal
        if (String.valueOf(deviceStateValueCurrent) != String.valueOf(deviceStateValueDesired)) {
            sendEvent(name: deviceStateName, value: deviceStateValueDesired, unit: deviceStateValueUnits)
            log("$prependLogMsg Device State Value for $deviceStateName changed: \"$deviceStateValueCurrent\" => \"$deviceStateValueDesired\"", TRACE)
        } else {
            log("$prependLogMsg Device State Value for $deviceStateName is unchanged: $deviceStateValueDesired", TRACE)
        }
    }
    
    log("$prependLogMsg END", DEBUG)
}

/**
 * Run when a request is issued to set the mode of the device
 *
 * @param mode manual, auto
 */

@groovy.transform.CompileStatic
@groovy.transform.TypeChecked
void setMode(String mode) {
    String prependLogMsg = "setMode($mode) -- "

    log("$prependLogMsg BEGIN", DEBUG)

    log("$prependLogMsg Setting Mode to $mode", INFO)

    onIfOff() // ensure device is on before sending the command
    sendWinixCommand("mode", mode)
    
    sendEvents()

    log("$prependLogMsg END", DEBUG)
}

/**
 * Run when a request is issued to set the plasmawave status of the device
 *
 * @param plasmawave on, off
 */

@groovy.transform.CompileStatic
@groovy.transform.TypeChecked
void setPlasmawave(String plasmawave) {
    String prependLogMsg = "setPlasmawave($plasmawave) -- "

    log("$prependLogMsg BEGIN", DEBUG)

    log("$prependLogMsg Setting Plasmawave to $plasmawave", INFO)

    sendWinixCommand("plasmawave", plasmawave)
    
    sendEvents()

    log("$prependLogMsg END", DEBUG)
}

/**
 * A sentence (must be terminated with a period, '.') describing at a high level what
 * this method does.  The first sentence will be part of the brief description that is
 * generated by the javadoc tool. Any additional text will be part of the detailed
 * description.
 *
 * @param logLevel
 *
 * @return A defscription of what value(s) are returned and under what specific
 * circumstances.
 */

@groovy.transform.CompileStatic
@groovy.transform.TypeChecked
int determineLogLevel(String logLevel) {
    return LOG_OPTIONS.indexOf(logLevel)
}

/**
 * A sentence (must be terminated with a period, '.') describing at a high level what
 * this method does.  The first sentence will be part of the brief description that is
 * generated by the javadoc tool. Any additional text will be part of the detailed
 * description.
 *
 * @param message
 *
 * @param logLevel
 */
void log(String message, String logLevel) {
    if (determineLogLevel(logLevel) >= determineLogLevel(settings.logging)) {
        log."${logLevel.toLowerCase()}" "$DRIVER_NAME (v$DRIVER_VERSION) -- ${device.name} -- ${device.label} -- $correlationID -- $message"
    }
}

/**
 * A sentence (must be terminated with a period, '.') describing at a high level what
 * this method does.  The first sentence will be part of the brief description that is
 * generated by the javadoc tool. Any additional text will be part of the detailed
 * description.
 */
void getWinixStatus() {
    String prependLogMsg = "getWinixStatus() --"

    log("$prependLogMsg BEGIN", DEBUG)
    
    Map requestParams = [uri: "https://us.api.winix-iot.com/common/event/sttus/devices/" + device.getDeviceNetworkId()]
    log("$prependLogMsg Winix Status API Request Params: " + requestParams, TRACE)

    // without this pause execution, the status does not appear to update
    //  status update from Winix is probably eventually consistent and requires a delay to update
    pauseExecution(settings.statusDelayInMS)
    
    httpGet(requestParams, {
        resp ->
        /*
        // Perfect Response
        {
          "statusCode": 200,
          "headers": {
            "resultCode": "S100",
            "resultMessage": ""
          },
          "body": {
            "deviceId": "ABCD1234_********",
            "totalCnt": 1,
            "data": [
              {
                "apiNo": "A210",
                "apiGroup": "001",
                "deviceGroup": "Air01",
                "modelId": "C545",
                "attributes": {
                  "A02": "1",
                  "A03": "02",
                  "A04": "01",
                  "A05": "01",
                  "A07": "1",
                  "A21": "4366",
                  "S07": "01",
                  "S08": "154",
                  "S14": "47"
                },
                "rssi": "-49",
                "creationTime": 1616516776523,
                "utcDatetime": "2021-03-23 16:26:16",
                "utcTimestamp": 1616516776
              }
            ]
          }
        }
        */
        
        int httpStatusCode = resp.getStatus()

        if (httpStatusCode == 200) {
            Map winixStatusResponse = resp.getData()
            log("$prependLogMsg Winix Status API Response Map: " + winixStatusResponse, TRACE)
            
            if (winixStatusResponse == WINIX_RESPONSE_BAD_DEVICE_ID) {
                // send event bad event id
                throw new Exception("$prependLogMsg Winix Response Bad Device ID")
            } else if (winixStatusResponse == WINIX_RESPONSE_DEVICE_NOT_CONNECTED) {
                // sent event device not connected
                throw new Exception("$prependLogMsg Winix Response Device Not Connected")
            } else if (winixStatusResponse == WINIX_RESPONSE_NO_DATA) {
                // sent event device no data
                throw new Exception("$prependLogMsg Winix Response No Data")    
            } else if (winixStatusResponse.statusCode == 200 && \
                       winixStatusResponse.headers.resultCode == "S100" && \
                       winixStatusResponse.headers.resultMessage == "" && \
                       winixStatusResponse.body.deviceId && \
                       winixStatusResponse.body.totalCnt == 1 && \
                       winixStatusResponse.body.data && \
                       winixStatusResponse.body.data[0]) {
                updateState(winixStatusResponse)
            } else {
                throw new Exception("$prependLogMsg Winix Status API Response Map: " + winixStateResponse)
            }
        } else {
            throw new Exception("$prependLogMsg Winix Status API Response Code: " + httpStatusCode)
        }
    })

    log("$prependLogMsg END", DEBUG)
}

void updateState(Map winixStatusResponse) {
    // send event bad device id false
    // send event device not connected false
    // send event no data false

    String prependLogMsg = "updateState($winixStatusResponse) --"

    log("$prependLogMsg BEGIN", DEBUG)

    Map winixStatus = winixStatusResponse.body.data[0]
    log("$prependLogMsg Winix State Map: " + winixStatus, TRACE)

    Map winixStatusAttributes = winixStatus.attributes
    log("$prependLogMsg Winix State Attributes Map: " + winixStatusAttributes, TRACE)

    // flattening the data structure to make it easier to iterate through
    winixStatus.remove("attributes")
    winixStatus += winixStatusAttributes
    log("$prependLogMsg Winix Status Flattened Map: " + winixStatus, TRACE)

    // check to see if map received is map expected
    Set winixStatusFlattenedMapKeys = winixStatus.keySet()
    log("$prependLogMsg Winix Status Flattened Map Keys:" + winixStatusFlattenedMapKeys, TRACE)

    Set expectedWinixStatuses = DEVICE.collect {
        it.value.attr.winixStatusKey
    }
    log("$prependLogMsg Expected Winix Statuses:" + expectedWinixStatuses, TRACE)

    // TODO: all expected attributes present
    //Set differenceOfAttributes = expectedWinixAttributes - winixStateFlattenedMapKeys
    //log.debug("Set Difference: " + differenceOfAttributes)

    // TODO: more attribues in response object than expected    
    //Set differenceOfAttributes2 = winixStateFlattenedMapKeys - expectedWinixAttributes
    //log.debug("Set Difference: " + differenceOfAttributes2)
    
    if (winixStatus['creationTime'] > device.currentValue('winixStateCreationTimeMS')) {
        // print log message that things have changed

        DEVICE.each {
            String deviceStateName, Map value ->
            // using def because this could be a string or integer
            def winixStatusValue = winixStatus[value.attr.winixStatusKey]
            log("$prependLogMsg Winix Status Value ($deviceStateName) : " + winixStatusValue, TRACE)

            // using def because this could be a string or integer
            def deviceStateValue = winixStatusValue

            // there is a lookup table
            if (value.attr.deviceToWinixLookup) {
                def deviceToWinixLookupEntry = value.attr.deviceToWinixLookup.find {
                    it.value == winixStatusValue
                }
                log("$prependLogMsg Device To Winix Lookup Entry ($deviceStateName): " + deviceToWinixLookup, TRACE)

                // if there is an entry for the corresponding winix value
                if (deviceToWinixLookupEntry) {
                    deviceStateValue = deviceToWinixLookupEntry.key
                } else {
                    log("$prependLogMsg Device to Winix Lookup Entry ($deviceStateName) does not exist", WARN)
                }
            } else {
                log("$prependLogMsg Device To Winix Lookup Map ($deviceStateName) does not exist.", TRACE)
            }
            
            updateStateValue(deviceStateName, deviceStateValue, value.attr.unitOfMeasure)
        }

        // create human readable date string
        updateStateValue("winixStateCreationDateString", new Date(winixStatus['creationTime'].longValue()).toString(), null)
    } else {
        // nothing has changed ... do nothing
    }   
    
    // TODO: Use DEVICE attribute lookup value
    updateStateValue("warnReplaceFilter", winixStatus['A21'].toLong() >= settings.filterReplacementInHours, null)
    
    // TODO: document
    updateStateValue("correlationID", correlationID, null)
    
    log("$prependLogMsg END", DEBUG)
}

void updateStateValue(String deviceStateName, def deviceStateValueDesired, String deviceStateValueUnits) {
    String prependLogMsg = "updateStateValue($deviceStateName, $deviceStateValueDesired, $deviceStateValueUnits) --"

    log("$prependLogMsg BEGIN", DEBUG)
        
    CachedStateLookupFor[deviceStateName] = [
        value: deviceStateValueDesired, 
        unit: deviceStateValueUnits
    ]
    
    log("$prependLogMsg END", DEBUG)
}

/**
 * A sentence (must be terminated with a period, '.') describing at a high level what
 * this method does.  The first sentence will be part of the brief description that is
 * generated by the javadoc tool. Any additional text will be part of the detailed
 * description.
 *
 * @param deviceStateName
 *
 * @param deviceStateDesiredValue
 */

void sendWinixCommand(String deviceStateName, String deviceStateDesiredValue) {
    String prependLogMsg = "sendWinixCommand($deviceStateName, $deviceStateDesiredValue) --"

    log("$prependLogMsg BEGIN", DEBUG)

    String winixStatusKey
    String winixStatusDesiredValue

    // allow a pass for "hc" or "health check".  this is a dummy call which will force the air purifier to sync its state to the cloud
    if (deviceStateName == "hc") {
        winixStatusKey = deviceStateName
        winixStatusDesiredValue = deviceStateDesiredValue
    } else {
        // check deviceStateName
        winixStatusKey = DEVICE[deviceStateName].attr.winixStatusKey
        winixStatusDesiredValue = DEVICE[deviceStateName].attr.deviceToWinixLookup[deviceStateDesiredValue]
    }

    Map requestParams = [uri: "https://us.api.winix-iot.com/common/control/devices/${device.getDeviceNetworkId()}/A211/$winixStatusKey:$winixStatusDesiredValue"]
    log("$prependLogMsg Winix Control API Request Params: " + requestParams, TRACE)

    httpGet(requestParams, {
        resp ->

        int httpStatusCode = resp.getStatus()

        if (httpStatusCode == 200) {
            Map winixControlResponse = resp.getData()
            log("$prependLogMsg Winix Control API Response Map: " + winixControlResponse, TRACE)
            
            if (winixControlResponse == WINIX_RESPONSE_BAD_DEVICE_ID) {
                throw new Exception("$prependLogMsg Winix Response Bad Device ID")
            } else if (winixControlResponse == WINIX_RESPONSE_DEVICE_NOT_CONNECTED) {
                throw new Exception("$prependLogMsg Winix Response Device Not Connected")
            } else if (winixControlResponse == WINIX_RESPONSE_NO_DATA) {
                throw new Exception("$prependLogMsg Winix Response No Data")    
            } else if (winixControlResponse == WINIX_RESPONSE_CONTROL_SUCCESS) {
                // test to see if the changes occurs and make changes to reflect state   
                getWinixStatus()
            } else {
                throw new Exception("$prependLogMsg Winix Control API Response Map: " + winixControlResponse)
            }
        } else {
            throw new Exception("$prependLogMsg Winix Control API Response Code: " + httpStatusCode)
        }
    })

    log("$prependLogMsg END", DEBUG)
}

Exception deviceException(Map winixControlResponse, String winixErrorMessage) {
    // winixErrorMessage
    // winixErrorTimeMS
    // winixErrorDateString
}

// sendWinixCommand
// getWinixStatus
// updateDeviceState