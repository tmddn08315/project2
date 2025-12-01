package org.spring.backendspring.rabbitmqWebsocket.chat.rabbitmqService;

import org.spring.backendspring.crew.crewBoard.repository.CrewBoardRepository;
import org.spring.backendspring.crew.crewRun.entity.CrewRunEntity;
import org.spring.backendspring.crew.crewRun.repository.CrewRunMemberRepository;
import org.spring.backendspring.crew.crewRun.repository.CrewRunRepository;
import org.spring.backendspring.rabbitmqWebsocket.chat.dto.BotMessageDto;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import kr.co.shineware.nlp.komoran.constant.DEFAULT_MODEL;
import kr.co.shineware.nlp.komoran.core.Komoran;
import kr.co.shineware.nlp.komoran.model.KomoranResult;
import kr.co.shineware.nlp.komoran.model.Token;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class MyCrewBotService {
    @Value("${spring.rabbitmq.crew.exchange}")
    private String crewExchangeYml;
    
    private final CrewRunRepository crewRunRepository;
    private final CrewBoardRepository crewBoardRepository;

    private final RabbitTemplate rabbitTemplate;
    private final Komoran komoran = new Komoran(DEFAULT_MODEL.FULL);

    //형태소 분석 오류 해결 
    private String komoranGoText(String komoranText) {
        if (komoranText == null) return "";
    
        // 시간 표현들 미리 정리
        komoranText = komoranText.replace("이번 주", "이번주");
        komoranText = komoranText.replace("저번 주", "저번주");
        komoranText = komoranText.replace("다음 주", "다음주");
        komoranText = komoranText.replace("이번 달", "이번달");
        komoranText = komoranText.replace("이번 달에", "이번달"); 
        komoranText = komoranText.replace("몇 개", "몇개"); 
    
        return komoranText;
    }

    public void sendCrewBot(BotMessageDto botMessageDto) {
        //사용자가 보낸 택스트 
        String komoranText = botMessageDto.getText() ;

        //위에 메서드
        String komoranGoGoText = komoranGoText(komoranText);

        //코모란
        KomoranResult komoranResult = komoran.analyze(komoranGoGoText);
        List<Token> tokens = komoranResult.getTokenList();

        //라우딩키인데 사실 별 의미는 없음 crew.#임 구독을 {crewId}.{memberId}로 해서
        String routingKey = "crew." + botMessageDto.getCrewId() + "." + botMessageDto.getMemberId();

        //봇 메시지
        String text = "";

        //기간설정,데이터찾기를 위한 선언 미리 하기
        LocalDate dateToday = LocalDate.now();
        LocalDateTime dateStart;
        LocalDateTime dateEnd;
        
        //if로 체크 하기위한 참 거짓
        boolean hi = false; //인사
        boolean today = false; //오늘
        boolean runSchedule = false; //런닝 일정
        boolean board = false; // 게시글
        // boolean botThis = false;
        boolean thisWeek = false; // 이번주
        boolean thisMonth = false;// 이번달
        boolean thisCount = false;// 몇개


        for (Token token : tokens) {
            String botMsgNnp = token.getMorph();
            log.info("====={}=====", botMsgNnp);
            
            // 초기 접속했을때나 인사
            if (List.of("안녕", "하이", "hello","hellow", "ㅎㅇ").contains(botMsgNnp)) hi = true;

            //시간
            if (botMsgNnp.equals("오늘")) today = true;
            // if (botMsgNnp.equals("이번")) botThis = true;
            if (List.of("이번주", "금주", "이번 ").contains(botMsgNnp)) thisWeek = true;
            if (botMsgNnp.equals("이번달")) thisMonth = true;

            //정보
            if (List.of("런","런닝", "일정").contains(botMsgNnp)) runSchedule = true;
            if (List.of("글", "게시글", "게시물").contains(botMsgNnp)) board = true;
        }
       
        if (hi) {
            text =  "어서오세요!" + botMessageDto.getMemberNickName() 
            + "님 궁금한 정보 있으시면 물어봐주세요 🚀" + "\n" +
            "오늘, 이번주, 이번달, 런닝, 일정밖에 키워드없음.. 아직은 ,,,";


            } else if (today && runSchedule) { // 오늘 런닝일정

                //시간대 설정    
                dateStart = dateToday.atStartOfDay();
                dateEnd = dateToday.plusDays(1).atStartOfDay();

                //일정 리스트
                List<CrewRunEntity> dateRunList =
                crewRunRepository.findByStartAtBetween(dateStart, dateEnd);

                if (dateRunList.isEmpty()) {
                    text =  "오늘런닝일정이 없어요" ;
                } else {
                    StringBuilder sb = new StringBuilder();
                    sb.append("오늘 런닝 일정은 총" +
                        dateRunList.size() + "개 있습니다" + "\n"+"\n") ;
                    for (CrewRunEntity run : dateRunList) {
                        
                        sb.append("시간 : " + run.getStartAt() + " ~ "+
                                 run.getEndAt() + "\n " +
                                "제목 : " + run.getTitle() + 
                                "장소 : " + run.getPlace() +
                                "코스 : " + run.getRouteHint()
                                +"\n" + "\n");
                    }
                    text = sb.toString();
                }
            } else if(thisWeek && runSchedule){ // 이번주 런닝일정
                //시간대 설정    
                LocalDate firstDayOfWeek = dateToday.with(DayOfWeek.MONDAY);   // 이번 주 월요일
                dateStart = firstDayOfWeek.atStartOfDay();      // 이번 주 월요일 0시
                dateEnd = firstDayOfWeek
                            .plusWeeks(1)                  // 다음 주 월요일
                            .atStartOfDay();

                //일정 리스트
                List<CrewRunEntity> dateRunList =
                crewRunRepository.findByStartAtBetween(dateStart, dateEnd);

                if (dateRunList.isEmpty()) {
                    text =  "이번주 런닝일정이 없어요" ;
                } else {
                    StringBuilder sb = new StringBuilder();
                    sb.append("이번주 런닝 일정은 총" +
                            dateRunList.size() + "개 있습니다" + "\n"+"\n") ;
                    for (CrewRunEntity run : dateRunList) {
                        
                        sb.append("시간 : " + run.getStartAt() + " ~ "+
                                 run.getEndAt() + "\n " +
                                "제목 : " + run.getTitle() + 
                                "장소 : " + run.getPlace() +
                                "코스 : " + run.getRouteHint()
                                +"\n" + "\n");
                    }
                    text = sb.toString();
                }
            } else if(thisMonth && runSchedule){ // 이번달 런닝일정
                LocalDate firstDayOfMonth = dateToday.withDayOfMonth(1);   // 이번 달 1일
                dateStart = firstDayOfMonth.atStartOfDay(); ;
                dateEnd = firstDayOfMonth
                            .plusMonths(1)             // 다음 달 1일
                            .atStartOfDay();           // 다음 달 1일 0시

                //일정 리스트
                List<CrewRunEntity> dateRunList =
                crewRunRepository.findByStartAtBetween(dateStart, dateEnd);

                if (dateRunList.isEmpty()) {
                    text =  "이번달 일정이 없어요" ;
                } else {
                    StringBuilder sb = new StringBuilder();
                    sb.append("이번달 런닝 일정은 총" +
                            dateRunList.size() + "개 있습니다" + "\n"+"\n") ;
                    for (CrewRunEntity run : dateRunList) {
                        
                        sb.append("시간 : " + run.getStartAt() + " ~ "+
                                 run.getEndAt() + "\n " +
                                "제목 : " + run.getTitle() + 
                                "장소 : " + run.getPlace() +
                                "코스 : " + run.getRouteHint()
                                +"\n" + "\n");
                    }
                    text = sb.toString();
                }
            } //추후에 더 추가예정 노다가라 힘들다
            else {
                    text = "등록 되어있지 않은 정보 입니다 :( "+
                    "\n"+
                    "추후에 더 추가 예정 노다가라 힘들다 ";
            }

        
        BotMessageDto botMessageDto2 = BotMessageDto.builder()
        .crewId(botMessageDto.getCrewId())
        .memberId(botMessageDto.getMemberId())
        .memberNickName(botMessageDto.getMemberNickName())
        .text(text)
        .build();

        rabbitTemplate.convertAndSend(crewExchangeYml, routingKey, botMessageDto2);
    }


}
