package com.capstone.bszip.Bookstore.service;

import com.capstone.bszip.Bookstore.domain.Bookstore;
import com.capstone.bszip.Bookstore.repository.BookstoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

import static com.capstone.bszip.Bookstore.domain.BookstoreCategory.*;

@Service
@RequiredArgsConstructor
public class OpenApiService {
    private final BookstoreRepository bookstoreRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private static final String API_URL_CAFE = "http://api.kcisa.kr/openapi/API_CIA_090/request";
    private static final String API_URL_INDEP = "http://api.kcisa.kr/openapi/API_CIA_089/request";
    private static final String API_URL_CHILD = "http://api.kcisa.kr/openapi/service/CNV/API_CNV_037";

    @Value("${bookstore.cafe.key}")
    private String BOOKSTORE_CAFE_KEY;
    @Value("${bookstore.indep.key}")
    private String BOOKSTORE_INDEP_KEY;
    @Value("${bookstore.child.key}")
    private String BOOKSTORE_CHILD_KEY;

    //@PostConstruct
    public void saveCafeData() {
        String urlCafe = UriComponentsBuilder.fromHttpUrl(API_URL_CAFE)
                .queryParam("serviceKey", BOOKSTORE_CAFE_KEY)
                .queryParam("numOfRows", 10) //테스트로 10개만
                .queryParam("pageNo", 1)
                .build()
                .toString();
        Map<String, Object> responseCafe = restTemplate.getForObject(urlCafe, Map.class);
        System.out.println(responseCafe);
        Map<String, Object> responseData = (Map<String, Object>) responseCafe.get("response");
        Map<String, Object> body = (Map<String, Object>) responseData.get("body");
        Map<String, Object> items = (Map<String, Object>) body.get("items");
        List<Map<String, Object>> dataList = (List<Map<String, Object>>) items.get("item");

        for (Map<String, Object> item : dataList) {
            Bookstore bookstore = new Bookstore();
            bookstore.setName((String) item.get("TITLE"));
            bookstore.setBookstoreCategory(CAFE);
            bookstore.setPhone((String) item.get("CONTACT_POINT"));
            bookstore.setHours((String) item.get("DESCRIPTION"));
            bookstore.setAddress((String) item.get("ADDRESS"));
            bookstore.setDescription((String) item.get("SUB_DESCRIPTION"));
            bookstoreRepository.save(bookstore);
        }
    }

    //@PostConstruct
    public void saveIndepData() {
        String urlIndep = UriComponentsBuilder.fromHttpUrl(API_URL_INDEP)
                .queryParam("serviceKey", BOOKSTORE_INDEP_KEY)
                .queryParam("numOfRows", 10) //테스트로 10개만
                .queryParam("pageNo", 1)
                .build()
                .toString();
        Map<String, Object> responseIndep = restTemplate.getForObject(urlIndep, Map.class);
        System.out.println(responseIndep);
        Map<String, Object> responseData = (Map<String, Object>) responseIndep.get("response");
        Map<String, Object> body = (Map<String, Object>) responseData.get("body");
        Map<String, Object> items = (Map<String, Object>) body.get("items");
        List<Map<String, Object>> dataList = (List<Map<String, Object>>) items.get("item");

        for (Map<String, Object> item : dataList) {
            Bookstore bookstore = new Bookstore();
            bookstore.setName((String) item.get("TITLE"));
            bookstore.setBookstoreCategory(INDEP);
            bookstore.setPhone((String) item.get("CONTACT_POINT"));
            bookstore.setHours((String) item.get("DESCRIPTION"));
            bookstore.setAddress((String) item.get("ADDRESS"));
            bookstore.setDescription((String) item.get("SUB_DESCRIPTION"));
            bookstoreRepository.save(bookstore);
        }
    }
    //@PostConstruct
    public void saveChildData() {
        String urlChild = UriComponentsBuilder.fromHttpUrl(API_URL_CHILD)
                .queryParam("serviceKey", BOOKSTORE_CHILD_KEY)
                .queryParam("numOfRows", 10) //테스트로 10개만
                .queryParam("pageNo", 1)
                .build()
                .toString();
        Map<String, Object> responseChild = restTemplate.getForObject(urlChild, Map.class);
        System.out.println(responseChild);
        Map<String, Object> responseData = (Map<String, Object>) responseChild.get("response");
        Map<String, Object> body = (Map<String, Object>) responseData.get("body");
        Map<String, Object> items = (Map<String, Object>) body.get("items");
        List<Map<String, Object>> dataList = (List<Map<String, Object>>) items.get("item");

        for (Map<String, Object> item : dataList) {
            Bookstore bookstore = new Bookstore();
            bookstore.setName((String) item.get("FCLTY_NM"));
            bookstore.setBookstoreCategory(CHILD);
            bookstore.setPhone("0" + (String) item.get("TEL_NO"));
            bookstore.setHours("평일개점마감시간" + convertDecimalToTime(item.get("WORKDAY_OPN_BSNS_TIME")) + "~" + convertDecimalToTime(item.get("WORKDAY_CLOS_TIME"))
                    + "토요일개점마감시간" + convertDecimalToTime(item.get("SAT_OPN_BSNS_TIME")) + "~" + convertDecimalToTime(item.get("SAT_CLOS_TIME"))
                    + "일요일개점마감시간" + convertDecimalToTime(item.get("SUN_OPN_BSNS_TIME")) + "~" + convertDecimalToTime(item.get("SUN_CLOS_TIME")));
            bookstore.setAddress((String) item.get("FCLTY_ROAD_NM_ADDR"));
            bookstore.setDescription((String) item.get("ADIT_DC"));
            bookstoreRepository.save(bookstore);
        }
    }
    public static String convertDecimalToTime(Object decimalTime) {
        if (decimalTime == null) return "";
        double time = Double.parseDouble(decimalTime.toString());
        int totalMinutes = (int) Math.round(time * 24 * 60);
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        return String.format("%02d:%02d", hours, minutes);
    }
}
