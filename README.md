TODO:
- register accessToken (in memcache) for logout if token expired
- remove all jobs tagged with access_token

- define possible filters
- add filter fields as separate columns: types, friend groups
- retrieve user names from uids on server and query them in the table

NEXT version:
- add iframe with fb login to get action links for deleting posts / making them private (if possible?)


1. show loading screen
2. check for facebook session
3. if logged in
	1. send access_token to API
	2. retrieve number of records for user in DB
		1. if 0 records, kick off map-timeline script and go to 3.3
		2. if has records, show two buttons: [show data] [reload data] and [status box]
		3. assign setInterval to get status (total number of records, records on page, data parsers running: yes/no, message console)
		4. when status is retrieved, enable show data
		5. [show data] goes to 3.3
		6. [reload data] goes to 3.2.1
	3. use KO to hide elements on page (hide login form, show header, show filters)
	4. [and] kick off data load with a 2 second delay to allow fb data to come through, order by create_time DESC
	5. on scroll, when reaching end of table, kick of another data load and add records to table
	6. filters are all preselected, when changing filter, hide rows from table
	7. when clicking on table headers (creation_date, owner_id) resort full JS table by that row
	8. if clicking on load full dataset, show loading screen, bring status box to center of screen and hide when complete, sort by creation_time DESC
	9. if clicking on refresh button, clear data table and reload by create_time
	10. if clicking on logout, send logout event / access_token to server, on confirm logout from facebook and hide reverse 3.3 (show login form, hide header, hide filters, delete data table, hide data table)
	11. if clicking on remove data from server, clear all user data from server
4. if not logged in
	1. show big login button
	2. on login confirmation, go to 3.1

FILTERS: has photo, privacy, type

has photo: YES/NO
privacy (multi select)::
	EVERYONE
	CUSTOM
	ALL_FRIENDS
	NETWORKS_FRIENDS
	FRIENDS_OF_FRIENDS
	?
type (multi select):
11 - Group created
12 - Event created
46 - Status update
56 - Post on wall from another user
66 - Note created
80 - Link posted
128 - Video posted
247 - Photos posted
237 - App story
257 - Comment created
272 - App story
285 - Checkin to a place
308 - Post in Group
