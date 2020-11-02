package io.mosip.authentication.common.service.integration;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.jayway.jsonpath.JsonPath;

import io.mosip.authentication.common.service.cache.MasterDataCache;
import io.mosip.authentication.core.constant.IdAuthCommonConstants;
import io.mosip.authentication.core.constant.IdAuthenticationErrorConstants;
import io.mosip.authentication.core.constant.RestServicesConstants;
import io.mosip.authentication.core.exception.IDDataValidationException;
import io.mosip.authentication.core.exception.IdAuthenticationBusinessException;
import io.mosip.authentication.core.indauth.dto.LanguageType;
import io.mosip.authentication.core.logger.IdaLogger;
import io.mosip.authentication.core.spi.indauth.match.IdInfoFetcher;
import io.mosip.kernel.core.logger.spi.Logger;

/**
 * MasterDataManager
 * 
 * @author Dinesh Karuppiah.T
 * @author Manoj SP
 */
@Component
public class MasterDataManager {

	private static final String TITLE_NAME_JSON_PATH = "$.response.titleList[?(@.langCode=='%s')].titleName";

	private static final String LANG_CODE_JSON_PATH = "$.response.titleList.*.langCode";

	private static final String LANG_CODE = "langCode";

	private static final String TEMPLATE_TYPE_CODE = "templateTypeCode";

	private static final String RESPONSE = "response";

	/** The Constant IS_ACTIVE. */
	private static final String IS_ACTIVE = "isActive";

	/** The Constant FILE_TEXT. */
	private static final String FILE_TEXT = "fileText";

	/** The Constant TEMPLATES. */
	private static final String TEMPLATES = "templates";

	/** The Constant NAME_PLACEHOLDER. */
	private static final String NAME_PLACEHOLDER = "$name";

	@Autowired
	private IdInfoFetcher idInfoFetcher;

	/**
	 * IdTemplate Manager Logger
	 */
	private static Logger logger = IdaLogger.getLogger(MasterDataManager.class);

	@Autowired
	private MasterDataCache masterDataHelper;

	/**
	 * Fetch master data.
	 *
	 * @param type               the type
	 * @param params             the params
	 * @param masterDataListName the master data list name
	 * @param keyAttribute       the key attribute
	 * @param valueAttribute     the value attribute
	 * @return the map
	 * @throws IdAuthenticationBusinessException the id authentication business
	 *                                           exception
	 */
	@SuppressWarnings("unchecked")
	Map<String, Map<String, String>> fetchMasterData(RestServicesConstants type, Map<String, String> params,
			String masterDataListName, String keyAttribute, String valueAttribute)
			throws IdAuthenticationBusinessException {
		try {
			Map<String, Object> response = masterDataHelper.getMasterDataTemplate(params.get(TEMPLATE_TYPE_CODE));

			Map<String, List<Map<String, Object>>> fetchResponse;
			if (response.get(RESPONSE) instanceof Map) {
				fetchResponse = (Map<String, List<Map<String, Object>>>) response.get(RESPONSE);
			} else {
				fetchResponse = Collections.emptyMap();
			}
			List<Map<String, Object>> masterDataList = fetchResponse.get(masterDataListName);
			Map<String, Map<String, String>> masterDataMap = new HashMap<>();
			for (Map<String, Object> map : masterDataList) {
				String langCode = String.valueOf(map.get(LANG_CODE));
				if (!params.containsKey(LANG_CODE)
						|| (params.containsKey(LANG_CODE) && langCode.contentEquals(params.get(LANG_CODE)))) {
					String key = String.valueOf(map.get(keyAttribute));
					String value = String.valueOf(map.get(valueAttribute));
					Object isActiveObj = map.get(IS_ACTIVE);
					if (isActiveObj instanceof Boolean && (Boolean) isActiveObj) {
						Map<String, String> valueMap = masterDataMap.computeIfAbsent(langCode,
								k -> new LinkedHashMap<String, String>());
						valueMap.put(key, value);
					}
				}
			}

			return masterDataMap;
		} catch (IDDataValidationException e) {
			logger.error(IdAuthCommonConstants.SESSION_ID, this.getClass().getName(), e.getErrorCode(),
					e.getErrorText());
			throw new IdAuthenticationBusinessException(IdAuthenticationErrorConstants.SERVER_ERROR, e);
		}
	}

	/**
	 * Fetch templates based on Language code and Template Name.
	 *
	 * @param langCode     the lang code
	 * @param templateName the template name
	 * @return the string
	 * @throws IdAuthenticationBusinessException the id authentication business
	 *                                           exception
	 */
	public String fetchTemplate(String langCode, String templateName) throws IdAuthenticationBusinessException {
		Map<String, String> params = new HashMap<>();
		params.put(LANG_CODE, langCode);
		params.put(TEMPLATE_TYPE_CODE, templateName);
		Map<String, Map<String, String>> masterData = fetchMasterData(
				RestServicesConstants.ID_MASTERDATA_TEMPLATE_SERVICE, params, TEMPLATES, TEMPLATE_TYPE_CODE, FILE_TEXT);
		return Optional.ofNullable(masterData.get(langCode)).map(map -> map.get(templateName)).orElse("");
	}

	/**
	 * To fetch template from master data manager.
	 *
	 * @param templateName the template name
	 * @return the string
	 * @throws IdAuthenticationBusinessException the id authentication business
	 *                                           exception
	 */
	public String fetchTemplate(String templateName) throws IdAuthenticationBusinessException {
		Map<String, String> params = new HashMap<>();
		String finalTemplate = "";
		StringBuilder template = new StringBuilder();
		params.put(TEMPLATE_TYPE_CODE, templateName);
		Map<String, Map<String, String>> masterData = fetchMasterData(
				RestServicesConstants.ID_MASTERDATA_TEMPLATE_SERVICE_MULTILANG, params, TEMPLATES, TEMPLATE_TYPE_CODE,
				FILE_TEXT);
		// Sort the list of entries based on primary lang/secondary lang order.
		// Here entry of primary lang should occur before secondary lang entry.
		List<Entry<String, Map<String, String>>> entries = masterData.entrySet().stream().sorted((o1, o2) -> {
			String lang1 = o1.getKey();
			String lang2 = o2.getKey();
			boolean lang1IsPrimary = lang1.equals(idInfoFetcher.getLanguageCode(LanguageType.PRIMARY_LANG));
			boolean lang2IsPrimary = lang2.equals(idInfoFetcher.getLanguageCode(LanguageType.PRIMARY_LANG));
			int val;
			if (lang1IsPrimary == lang2IsPrimary) {
				val = 0;
			} else {
				val = lang1IsPrimary ? -1 : 1;
			}
			return val;
		}).collect(Collectors.toList());
		for (Iterator<Entry<String, Map<String, String>>> iterator = entries.iterator(); iterator.hasNext();) {
			Entry<String, Map<String, String>> value = iterator.next();
			Map<String, String> valueMap = value.getValue();
			String lang = value.getKey();
			if (lang.equals(idInfoFetcher.getLanguageCode(LanguageType.PRIMARY_LANG))
					|| lang.equals(idInfoFetcher.getLanguageCode(LanguageType.SECONDARY_LANG))) {
				finalTemplate = (String) valueMap.get(templateName);
				if (finalTemplate != null) {
					finalTemplate = finalTemplate.replace(NAME_PLACEHOLDER, NAME_PLACEHOLDER + "_" + lang);
					template.append(finalTemplate);
				} else {
					throw new IdAuthenticationBusinessException(
							IdAuthenticationErrorConstants.UNABLE_TO_PROCESS.getErrorCode(),
							IdAuthenticationErrorConstants.UNABLE_TO_PROCESS.getErrorMessage()
									+ " - template not found: " + templateName);
				}
			}
			if (iterator.hasNext()) {
				template.append("\n\n");
			}
		}

		return template.toString();

	}

	/**
	 * To fetch titles.
	 *
	 * @return the map
	 * @throws IdAuthenticationBusinessException the id authentication business
	 *                                           exception
	 */
	@SuppressWarnings("unchecked")
	public Map<String, List<String>> fetchTitles() throws IdAuthenticationBusinessException {
		Map<String, Object> fetchMasterData = masterDataHelper.getMasterDataTitles();
		List<String> langCodes = ((List<String>) JsonPath.compile(LANG_CODE_JSON_PATH).read(fetchMasterData));
		langCodes = langCodes.stream().collect(Collectors.toSet()).stream().collect(Collectors.toList());
		return langCodes.stream().map(langCode -> new AbstractMap.SimpleEntry<String, List<String>>(langCode,
				(List<String>) JsonPath.compile(String.format(TITLE_NAME_JSON_PATH, langCode)).read(fetchMasterData)))
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue));
	}

}
