/*
  Copyright 2022 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
 */

package com.adobe.marketing.mobile.target;

import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.JSONUtils;
import com.adobe.marketing.mobile.util.StringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

class TargetResponseParser {

	private static final String CLASS_NAME = "TargetResponseParser";

	/**
	 * Extracts the raw server response and returns it as a {@code Map} containing the
	 * response data from the Target server.
	 * <p>
	 * Returns null if the provided {@code serverResponseJson} is null.
	 *
	 * @param serverResponseJson A {@link JSONObject} server response.
	 * @return A {@code List<Map<String, Object>>} containing the response data.
	 * @throws {@link JSONException}
	 */
	Map<String, Object> extractRawResponse(final JSONObject serverResponseJson) throws JSONException {
		if (serverResponseJson == null) {
			return null;
		}

		return JSONUtils.toMap(serverResponseJson);
	}

	/**
	 * Extracts the mboxes from the server response for a certain key.
	 * <p>
	 * Used by methods {@code #extractPrefetchedMboxes(JSONObject)} and {@code #extractBatchedMBoxes(JSONObject)}.
	 * Do not pass a null {@code JSONObject} serverResponseJson.
	 *
	 * @param serverResponseJson A {@link JSONObject} server response
	 * @param key A {@link String} key name for which mbox need should the mboxes be extracted from
	 * @return all the mboxes for the given key
	 */
	private JSONArray getMboxesFromKey(final JSONObject serverResponseJson, final String key) {
		JSONObject containerJson = serverResponseJson.optJSONObject(key);

		if (containerJson == null) {
			Log.debug(TargetConstants.LOG_TAG, CLASS_NAME, "getMboxesFromKey - Unable to retrieve mboxes from key, json is null");
			return null;
		}

		JSONArray mboxJSONArray = containerJson.optJSONArray(TargetJson.MBOXES);

		if (mboxJSONArray == null) {
			Log.debug(TargetConstants.LOG_TAG, CLASS_NAME, "getMboxesFromKey - Unable to retrieve mboxes from key, mboxes array is null");
			return null;
		}

		return mboxJSONArray;
	}

	/**
	 * Extracts the batched mboxes from the server response and returns them as a {@code Map}, where the mbox name is the key
	 * and the {@code JSONObject} returned from the server is the value.
	 * <p>
	 * Returns null if there is no {@link TargetJson#MBOX_RESPONSES} key found in the server response.
	 * Do not pass a null {@code JSONObject} serverResponseJson.
	 *
	 * @param serverResponseJson A {@link JSONObject} server response
	 * @return A {@link Map} of all the batched mboxes
	 */
	Map<String, JSONObject> extractBatchedMBoxes(final JSONObject serverResponseJson) {
		JSONArray batchedMboxes = getMboxesFromKey(serverResponseJson, TargetJson.EXECUTE);

		if (batchedMboxes == null) {
			return null;
		}

		Map<String, JSONObject> mboxResponses = new HashMap<String, JSONObject>();

		for (int i = 0; i < batchedMboxes.length(); i++) {
			JSONObject mboxJson = batchedMboxes.optJSONObject(i);

			if (mboxJson == null) {
				continue;
			}

			final String mboxName = mboxJson.optString(TargetJson.Mbox.NAME, "");

			if (StringUtils.isNullOrEmpty(mboxName)) {
				continue;
			}

			mboxResponses.put(mboxName, mboxJson);
		}

		return mboxResponses;
	}

	/**
	 * Extracts the prefetched mboxes from the server response and returns them as a {@code Map}, where the mbox name is the key
	 * and the {@code JSONObject} returned from the server is the value.
	 * <p>
	 * Returns null if there is no {@code TargetJson#PREFETCH_MBOX_RESPONSES} key found in the server response.
	 * Do not pass a null {@code JSONObject} serverResponseJson.
	 *
	 * @param serverResponseJson A {@link JSONObject} server response
	 * @return A {@link Map} of all the prefetched mboxes
	 */
	Map<String, JSONObject> extractPrefetchedMboxes(final JSONObject serverResponseJson) {
		JSONArray prefetchedMboxes = getMboxesFromKey(serverResponseJson, TargetJson.PREFETCH);

		if (prefetchedMboxes == null) {
			return null;
		}

		Map<String, JSONObject> mboxResponses = new HashMap<String, JSONObject>();

		for (int i = 0; i < prefetchedMboxes.length(); i++) {
			JSONObject mboxJson = prefetchedMboxes.optJSONObject(i);

			if (mboxJson == null) {
				continue;
			}

			final String mboxName = mboxJson.optString(TargetJson.Mbox.NAME, "");

			if (StringUtils.isNullOrEmpty(mboxName)) {
				continue;
			}

			Iterator<String> keyIterator = mboxJson.keys();
			List<String> keyCache = new ArrayList<String>();

			while (keyIterator.hasNext()) {
				keyCache.add(keyIterator.next());
			}

			for (String key : keyCache) {
				if (!TargetJson.CACHED_MBOX_ACCEPTED_KEYS.contains(key)) {
					mboxJson.remove(key);
				}
			}

			mboxResponses.put(mboxName, mboxJson);
		}

		return mboxResponses;
	}

	/**
	 * Extracts the prefetched views from the server response and returns them as a JSON {@code String}
	 * <p>
	 * Returns null if there is no {@link TargetJson#VIEWS} key found in the server prefetch response.
	 * Do not pass a null {@code JSONObject} serverResponseJson.
	 *
	 * @param serverResponseJson A {@link JSONObject} server response
	 * @return A {@link String} containing the prefetched views JSON
	 */
	String extractPrefetchedViews(final JSONObject serverResponseJson) {
		if (serverResponseJson == null) {
			Log.debug(TargetConstants.LOG_TAG, CLASS_NAME,
					  "extractPrefetchedViews - unable to extract prefetch views, server response is null");
			return null;
		}

		final JSONObject containerJson = serverResponseJson.optJSONObject(TargetJson.PREFETCH);

		if (containerJson == null) {
			Log.debug(TargetConstants.LOG_TAG, CLASS_NAME,
					"extractPrefetchedViews - unable to extract prefetch views, container json is null");
			return null;
		}

		final JSONArray viewsJSONArray = containerJson.optJSONArray(TargetJson.VIEWS);

		if (viewsJSONArray == null || viewsJSONArray.length() == 0) {
			Log.debug(TargetConstants.LOG_TAG, CLASS_NAME,
					"extractPrefetchedViews - unable to extract prefetch views, views array is null");
			return null;
		}

		return viewsJSONArray.toString();
	}

	/**
	 * Get the tnt id from the {@code JSONObject} server response.
	 * <p>
	 * Returns null if there is no {@code TargetJson#ID} key found in the server response.
	 * Do not pass a null {@code JSONObject} serverResponseJson.
	 *
	 * @param serverResponseJson  A {@link JSONObject} server response
	 * @return A {@link String} tntid
	 */
	String getTntId(final JSONObject serverResponseJson) {
		JSONObject idJson = serverResponseJson.optJSONObject(TargetJson.ID);

		if (idJson == null) {
			return null;
		}

		return idJson.optString(TargetJson.ID_TNT_ID, null);
	}

	/**
	 * Get the edge host from the {@code JSONObject} server response
	 * <p>
	 * Returns an empty {@code String} if there is no {@code TargetJson#EDGE_HOST} key found in the server response.
	 * Do not pass a null {@code JSONObject} serverResponseJson.
	 *
	 * @param serverResponseJson A {@link JSONObject} server response
	 * @return A {@link String} edge host
	 */
	String getEdgeHost(final JSONObject serverResponseJson) {
		return serverResponseJson.optString(TargetJson.EDGE_HOST, "");
	}


	/**
	 * Grabs the a4t payload from the target response and convert the keys to correct format
	 * <p>
	 * Returns null if there is no analytics payload that needs to be sent.
	 *
	 * @param mboxJson A prefetched mbox {@link JSONObject}
	 * @return A {@link Map} containing a4t payload
	 */
	Map<String, String> getAnalyticsForTargetPayload(final JSONObject mboxJson, final String sessionId) {
		final Map<String, String> payload = getAnalyticsForTargetPayload(mboxJson);
		return preprocessAnalyticsForTargetPayload(payload, sessionId);
	}

	/**
	 * Converts the A4T params keys to correct format as required by the Analytics
	 * <p>
	 * Returns null if {@code payload} is null or empty
	 *
	 * @param payload {@code Map<String, String>} with A4t params
	 * @param sessionId Target session id
	 * @return {@code Map<String, String>} with processed keys
	 */
	Map<String, String> preprocessAnalyticsForTargetPayload(final Map<String, String> payload, final String sessionId) {
		if (payload == null || payload.isEmpty()) {
			return null;
		}

		final Map<String, String> modifiedPayload = new HashMap<String, String>();

		for (Map.Entry<String, String> entry : payload.entrySet()) {
			modifiedPayload.put("&&" + entry.getKey(), entry.getValue());
		}

		if (!StringUtils.isNullOrEmpty(sessionId)) {
			modifiedPayload.put(TargetConstants.EventDataKeys.A4T_SESSION_ID, sessionId);
		}

		return modifiedPayload;
	}

	/**
	 * Parse the JSON object parameter to read A4T payload and returns it as a {@code Map<String, String>}.
	 * Returns null if {@code json} doesn't have A4T payload.
	 *
	 * @param json {@link JSONObject} containing analytics payload
	 * @return {@code Map<String, String>} containing A4T params
	 */
	Map<String, String> getAnalyticsForTargetPayload(final JSONObject json) {
		if (json == null) {
			return null;
		}

		final JSONObject analyticsJson = json.optJSONObject(TargetJson.ANALYTICS_PARAMETERS);

		if (analyticsJson == null) {
			return null;
		}

		final JSONObject payloadJson = analyticsJson.optJSONObject(TargetJson.ANALYTICS_PAYLOAD);

		if (payloadJson == null) {
			return null;
		}

		// todo
		return mapFromJsonObject(payloadJson);
	}

	/**
	 * Parse the Mbox JSON object to read Response Tokens from the Options. The data will be read from the first option in the options list.
	 *
	 * @param mboxJson Mbox {@link JSONObject}
	 * @return Response Tokens from options payload as {@code Map<String, String>} OR null if Response Tokens are not activated on Target.
	 */
	Map<String, String> getResponseTokens(final JSONObject mboxJson) {
		if (mboxJson == null) {
			return null;
		}

		final JSONArray optionsArray = mboxJson.optJSONArray(TargetJson.OPTIONS);

		if (optionsArray == null || optionsArray.length() == 0) {
			return null;
		}

		// Mbox payload will have a single option object in options array, which is accessed using the index 0 and
		// further used to grab response tokens.
		final JSONObject option = optionsArray.optJSONObject(0);

		if (option == null) {
			return null;
		}

		final JSONObject responseTokens = option.optJSONObject(TargetJson.Option.RESPONSE_TOKENS);

		if (responseTokens == null) {
			return null;
		}

		// todo
		return mapFromJsonObject(responseTokens);
	}

	/**
	 * Extracts the click metric A4T params from the {@code mboxJson} and return them as a {@code Map<String, String>}.
	 * <p>
	 * Returns null if click metric analytics payload is missing in the mbox payload or there is any error in parsing {@code mboxJson}.
	 *
	 * @param mboxJson {@link JSONObject} of a mbox
	 * @return {@code Map<String, String>} containing click metric A4T params
	 */
	Map<String, String> extractClickMetricAnalyticsPayload(final JSONObject mboxJson) {
		JSONObject clickMetric = getClickMetric(mboxJson);
		return getAnalyticsForTargetPayload(clickMetric);
	}

	/**
	 * Grab the click metric {@link JSONObject} and returns it
	 * <p>
	 * This method returns null if the input {@code mboxJson} is null,
	 * or if the metrics array is not present in the {@code mboxJson},
	 * or if a valid click metric object is not found in the metrics array,
	 * or if the eventToken is not found in the click metric object
	 *
	 * @param mboxJson {@code JSONObject} for mbox
	 * @return {@code JSONObject} for click metric
	 */
	JSONObject getClickMetric(final JSONObject mboxJson) {
		if (mboxJson == null) {
			return null;
		}

		final JSONArray metricsArray = mboxJson.optJSONArray(TargetJson.METRICS);

		if (metricsArray == null || metricsArray.length() == 0) {
			return null;
		}

		JSONObject clickMetric = null;

		for (int i = 0; i < metricsArray.length(); i++) {
			final JSONObject metric = metricsArray.optJSONObject(i);

			if (metric == null ||
					!TargetJson.MetricType.CLICK.equals(metric.optString(TargetJson.Metric.TYPE, null))
					|| StringUtils.isNullOrEmpty(metric.optString(TargetJson.Metric.EVENT_TOKEN, null))) {
				continue;
			}

			clickMetric = metric;
			break;
		}

		return clickMetric;
	}

	/**
	 * Return the Target error message, if any
	 *
	 * @param responseJson {@link JSONObject} Target response JSON
	 * @return {@link String} response error, if any
	 */
	String getErrorMessage(final JSONObject responseJson) {
		if (responseJson == null) {
			return null;
		}

		return responseJson.optString(TargetJson.MESSAGE, null);
	}

	/**
	 * Return Mbox content from mboxJson, if any
	 *
	 * @param mboxJson {@link JSONObject} Target response JSON
	 * @return {@link String} mbox content, if any otherwise returns null
	 */
	String extractMboxContent(final JSONObject mboxJson) {
		if (mboxJson == null) {
			Log.debug(TargetConstants.LOG_TAG, CLASS_NAME,
					"extractMboxContent - unable to extract mbox contents, mbox json is null");
			return null;
		}

		JSONArray optionsArray = mboxJson.optJSONArray(TargetJson.OPTIONS);

		if (optionsArray == null) {
			Log.debug(TargetConstants.LOG_TAG, CLASS_NAME,
					"extractMboxContent - unable to extract mbox contents, options array is null");
			return null;
		}

		StringBuilder contentBuilder = new StringBuilder();

		for (int i = 0; i < optionsArray.length(); i++) {
			JSONObject option = optionsArray.optJSONObject(i);

			if (option == null || StringUtils.isNullOrEmpty(option.optString(TargetJson.Option.CONTENT, ""))) {
				continue;
			}

			final String optionType = option.optString(TargetJson.Option.TYPE, "");
			String optionContent = "";

			if (optionType.equals(TargetJson.HTML)) {
				optionContent = option.optString(TargetJson.Option.CONTENT, "");
			} else if (optionType.equals(TargetJson.JSON)) {
				JSONObject contentJSON = option.optJSONObject(TargetJson.Option.CONTENT);

				if (contentJSON != null) {
					optionContent = contentJSON.toString();
				}
			}

			contentBuilder.append(optionContent);
		}

		return contentBuilder.toString();
	}
}