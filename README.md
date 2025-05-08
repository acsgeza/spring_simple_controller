# TimeSeries

Contains the implementation of a small web application.\
The application can greet you in a selected language.
The languages and greetings can be managed by a fully implemented CRUD REST API.

This web application is only for showing the desired technologies to use.

## Run the server

Build the project with `mvn clean install`.

When it is ready you can run it with `java -jar target/TimeSeries-0.0.1-SNAPSHOT.jar`.\
This will launch the server on `localhost:8080`

## Run frontend

Consult with the frontend [README.md](frontend/time-series-app/README.md) on how to start the frontend app.

The backend URL is set inside the project, if you start the server and then the frontend they will be able to communicate with each other.

## The task

Write a web application using the pre-existing structure.\
(You can remove the current code, it only serves as an example.)

The task has several parts. Each part is a new feature request you can add to the program.\
The features are ordered by how they block each other and significance. (The least significant feature is the last.)\
Try to implement as much as you can. It is recommended to read every feature request first.

Some of the tasks have optional features. You can skip those or implement them depending on your time.

### 1. Receive time series files

*It is very common in this field to handle time-series, which are an array of values with some identification.
The values are always interpreted in order, so the nth value is before the n+1th value. In this feature you need to process a power-station's estimated 15 minute average production as time-series.*

Create a server endpoint which can process JSON files of the following format:
```json
{
  "power-station": "Solar Power Plant Kft. Nemesmedves",
  "date": "2021-06-28",
  "zone": "Europe/Budapest",
  "timestamp": "2021-06-28 12:16:43",
  "period": "PT15M",
  "series": [ 
    0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
    4000,4000,8000,12000,16000,24000,40000,56000,88000,
    104000,76000,124000,140000,104000,160000,144000,324000,
    348000,360000,376000,308000,432000,444000,444000,452000,
    456000,476000,464000,476000,480000,480000,476000,404000,
    316000,396000,308000,288000,468000,324000,368000,452000,
    324000,368000,364000,368000,368000,256000,264000,264000,
    172000,152000,140000,168000,124000,80000,60000,40000,
    24000,20000,20000,12000,4000,0,0,0,0,0,0,0,0,0,0,0,0,0,0
  ]
}
```
* `power-station` is the unique name of the PV site. Essentially an identifier.
* `date` is the local date (day) for the time-series always interpreted in the timezone described by the `zone` field.
* `timestamp` is the creation timestamp of the data always interpreted in UTC timezone.
* `period` is the resolution of the time series.
* `series` is the actual data. The number of values in the series equals to the length of the day marked by `date` in minutes divided by the `period`.\
On a normal day for 15-minute period it should be 96. This could be different on DST days.
  
>**Note:** Example time-series files could be found in this repository under `sample_data` folder.
 
While there is no frontend to try out the endpoint you can test it with the `curl` tool:
```shell
curl -X PUT -H "Content-Type: application/json" \
  -d @sample_data/ps_159_20210628_033004.json \
  http://localhost:8080/api/time-series
```
  
### 2. Store the time-series inside the database

*If an estimation was received via the endpoint you need to store it for later use in order to run analysis and other algorithms on it.*

The `power-station` and the `date` are unique for a time-series.
> One power-station could have time-series for multiple dates.\
> On a date multiple power-station could have time-series.\
> But for one date one power-station could only have one time-series.

Come up with an optimal storing strategy. Create the entities and repositories if necessary.\
Using the endpoint store the time-series data on its arrival.

### 3. Handle different versions of the same time-series

*An estimation could be changed at any time of the day.
If we want to change the estimation we would send a new time-series with the same `date` and `power-station`.\
The statement in the 2nd feature request is true, but a time-series could have multiple versions throughout the day.*

The server needs to receive multiple time-series for the same day and for the same power-station.\
In order to handle this the time-series versioning should be implemented.

For a new time-series version the `power-station` and the `date` will be the same but the `series` and the `timestamp` could be different.\
Store different versions of the same time-series in the database.\
Associate a version number with version. The version number should be monotonically increasing, so a bigger version number represents a later file.
>**Note**: The `timestamp` field represents the arrival time. It is there to "simulate" the actual time.\
> According to this the `timestmap` field of continuously received time-series should be ever increasing
> 
> **Optionally:** you can filter time-series where the `timestmap` is before the latest received `timestamp`. 


### 4. Handle safety window

*An estimation is always for the rest of the day from the receiving time (represented by `timestamp`).
Also there is a window in the future where the estimation could not be changed.
This serves the convenience, and the reliability of the organization which handles these time-series.
To simulate that a new version of a time-series must be merged with previous version incorporating the `timestmap` and the safety window size.*

Time-series can not be changed in the past and inside a certain safety window.\
When a new version of a series arrives take the `timestamp` value and consider it as the actual time.\
Before the actual time and inside the safety window the time-series can not be changed.

Based on the current series, and the series with the previous (latest) version, 
the saved series should be a copy of the previous version series, 
with new values taken from the current series after the safety window.

Increase the version number and store the merged time-series as the actual.
```
series_v1 = [1, 1, 1, 1, 1, 1, 1, 1, 1, 1]
                ^           ^
                |           |
            value at    value at
            current     the end of
            timestamp   the safety
                        window
series_v2 = [2, 2, 2, 2, 2, 2, 2, 2, 2, 2]
// then the new series should be a merge of the two:          
series_m  = [1, 1, 1, 1, 1, 1, 2, 2, 2, 2]
                            ^
                            |
                        until this value
                        same as v1
```
Create a parameter for the safety window of 1.5 hours.
>**Note**: The `timestamp` is not necessarily on period. If the `timestamp` passes a period the safety should be calculated from the next period.\
> This means you have to calculate a period from the `timestamp`.

## 5. Create a file upload frontend component
Create a mechanism to upload any time-series files via frontend, using the endpoint that you created before.\
You can use any technique to your preference (drag-and-drop, single select, multi select).
>**Note:** Example time-series files can be found in this repository under `sample_data` folder.

## 6. Show time-series
Create an endpoint for power-stations, and show it on the frontend.\
If a power-station is selected show the available `dates`. Available means that the power-station has time-series for that date.\
If a date is selected show all time-series in a table:

Solar Power Plant Kft. Nemesmedves - 2021-06-28

| period end | ver1                | ver2                | ver3                | ... |
| ---------- | ------------------- | ------------------- | ------------------- | --- |
|            | 2021-06-28 00:07:01 | 2021-06-28 00:29:45 | 2021-06-28 00:45:37 | ... |
| 00:15      | 1                   | 1                   | 1                   |
| 00:30      | 1                   | 1                   | 1                   |
| 00:45      | 1                   | 1                   | 1                   |
| 01:00      | 1                   | 1                   | 1                   |
| 01:15      | 1                   | 1                   | 1                   |
| 01:30      | 1                   | 1                   | 1                   |
| 01:45      | 1                   | 1                   | 1                   |
| 02:00      | 1                   | 1                   | 1                   |
| 02:15      | 1                   | 2                   | 2                   |
| 02:30      | 1                   | 2                   | 2                   |
| 02:45      | 1                   | 2                   | 2                   |
| 03:00      | 1                   | 2                   | 3                   |
| 03:15      | 1                   | 2                   | 3                   |
| 03:30      | 1                   | 2                   | 3                   |
| 03:40      | 1                   | 2                   | 3                   |
| ...        | 1                   | 2                   | 3                   |
| 23:30      | 1                   | 2                   | 3                   |
| 23:45      | 1                   | 2                   | 3                   |

>*Optionally:* Mark the values covered by safety window

# 7. Persistent database
Change the currently used in memory database to a persistent one.\
The desired feature is that after a server restart, the previously received time-series should be present.

> *Note*: In our technology stack we use MySQL, but you can use any persistent database to your preference.

# 8. Containerization
Create a self containing releasable package for the application.
The desired feature is when someone wants to start the application, then it could be done with a single command.\
Containers should be created for the database, server and frontend. 

> *Note*: In our technology stack we use Docker, but you can use any containerization tool to your preference.
