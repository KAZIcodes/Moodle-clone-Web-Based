package com.drproject.service;

import com.drproject.entity.*;
import com.drproject.repository.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
public class ARservice {
    private final ARRepository arRepository;
    private final UserRepository userRepository;
    private final ClassroomRepository classroomRepository;
    private final SectionRepository sectionRepository;
    private final QuestionRepository questionRepository;
    private final ChoiceRepository choiceRepository;
    private final StudentLongAnswerRepository studentLongAnswerRepository;
    private final LongAnswerRepository longAnswerRepository;

    public ARservice(ARRepository arRepository, UserRepository userRepository, ClassroomRepository classroomRepository, SectionRepository sectionRepository, QuestionRepository questionRepository, ChoiceRepository choiceRepository, StudentLongAnswerRepository studentLongAnswerRepository, LongAnswerRepository longAnswerRepository) {
        this.arRepository = arRepository;
        this.userRepository = userRepository;
        this.classroomRepository = classroomRepository;
        this.sectionRepository = sectionRepository;
        this.questionRepository = questionRepository;
        this.choiceRepository = choiceRepository;
        this.studentLongAnswerRepository = studentLongAnswerRepository;
        this.longAnswerRepository = longAnswerRepository;
    }

    public HashMap<String, Object> getAR(String uuid){
        HashMap<String, Object> res = new HashMap<>();
        if(arRepository.existsById(uuid)){
            res.put("obj",arRepository.getActivityById(uuid));
            res.put("msg","activity object found and returned successfully");
            res.put("status",true);
            return res;
        }
        res.put("obj",null);
        res.put("msg", "failed to find activity object");
        res.put("status", false);
        return res;
    }

    public HashMap<String, Object> getARdataTeacher(String uuid){
        // get student grades for quiz
        HashMap<String, Object> res = new HashMap<>();
        List<HashMap<String, String>> out = new ArrayList<>();
        if(arRepository.existsById(uuid)){
            Quiz q = (Quiz) arRepository.getActivityById(uuid);
            List<StudentLongAnswer> studentLongAnswers = q.getStudentLongAnswers();
            for(StudentLongAnswer studentLongAnswer : studentLongAnswers){
                HashMap<String ,String> studentGrade = new HashMap<>();
                studentGrade.put("firstName",studentLongAnswer.getUser().getFirstName());
                studentGrade.put("lastName", studentLongAnswer.getUser().getLastName());
                studentGrade.put("username", studentLongAnswer.getUser().getUsername());
                getStudentGradeForActivity(studentLongAnswer.getUser().getUsername(), uuid);
                studentGrade.put("grade", studentLongAnswer.getGrade());
                out.add(studentGrade);
            }
            res.put("obj", out);
            res.put("msg", "successfully returned student grades");
            res.put("status", true);
            return res;
        }
        res.put("obj", null);
        res.put("msg", "activity does not exist");
        res.put("status", false);
        return res;
    }
    public HashMap<String, Object> getAllGradesForClassroom(String classroomCode){
        HashMap<String, Object> res = new HashMap<>();
        if(classroomRepository.existsByCode(classroomCode)) {
            Classroom classroom = classroomRepository.getClassroomByCode(classroomCode);
            List<HashMap<String,String>> outList = new ArrayList<>();
            for(ClassroomRole c : classroom.getRoleInClassrooms()){
                String firstName = c.getUser().getFirstName();
                String lastName = c.getUser().getLastName();
                String grade = (String) getStudentGradeForClass(c.getUser().getUsername(),classroomCode).get("obj");
                HashMap<String,String> out = new HashMap<>();
                out.put("firstName", firstName);
                out.put("lastName", lastName);
                out.put("grade", grade);
                outList.add(out);
            }
            res.put("msg", "successfully returned classroom grades");
            res.put("obj", outList);
            res.put("status", true);
            return res;
        }
        else {
            res.put("status", false);
            res.put("msg", "classroom not found");
            res.put("obj", null);
            return res;
        }
    }

    public HashMap<String, Object> getStudentGradeForClass(String username, String classroomCode){
        HashMap<String, Object> res = new HashMap<>();
        User user = userRepository.getUserByUsername(username);
        double score = 0;

        if(classroomRepository.existsByCode(classroomCode)){
            Classroom classroom = classroomRepository.getClassroomByCode(classroomCode);
            List<Section> sections = classroom.getSections();
            for(Section s: sections){
                HashMap<String,Object> hashMap = getStudentGradeForSection(username,classroomCode,s.getId());
                if (hashMap.get("obj").equals(null)){
                    score += 0;
                }
                else {
                    score += Double.parseDouble((String) hashMap.get("obj"));
                }
            }
            if(classroom.getSections().size()==0) {
                res.put("obj", "0");
                res.put("msg", "classroom score for user returned successfully");
                res.put("status", true);
                return res;
            }
            String out = "" + (score/classroom.getSections().size());
            res.put("obj", out);
            res.put("msg", "classroom score for user returned successfully");
            res.put("status", true);
            return res;
        }
        res.put("obj", null);
        res.put("msg", "classroom doesn't exist");
        res.put("status", true);
        return res;
    }

    public HashMap<String,Object> getStudentGradeForSection(String username, String classroomCode, String sectionUUID){
        HashMap<String, Object> res = new HashMap<>();
        User user = userRepository.getUserByUsername(username);
        double score = 0;

        if(classroomRepository.existsByCode(classroomCode)){
            Classroom classroom = classroomRepository.getClassroomByCode(classroomCode);
            List<Section> sections = classroom.getSections();
            for(Section s: sections) {
                if (s.getId().equals(sectionUUID)) {
                    List<Activity> sectionActivities = s.getActivities();
                    for (Activity a : sectionActivities) {
                        HashMap<String, Object> hashMappedRes = getStudentGradeForActivity(username, a.getId());
                        score += Double.parseDouble((String) hashMappedRes.get("obj"));
                    }
                }
            }
            if (classroom.getSections().size()==0){
                res.put("obj", "0");
                res.put("msg", "returned user score in section");
                res.put("status", true);
                return res;
            }
            String out = "" + (score / classroom.getSections().size());
            res.put("obj", out);
            res.put("msg", "returned user score in section");
            res.put("status", true);
            return res;
        }
        else {
            res.put("msg", "classroom not found");
            res.put("status", false);
            res.put("obj",null);
            return res;
        }
    }

    public HashMap<String,Object> getStudentGradeForActivity(String username, String uuid){
        HashMap<String, Object> res = new HashMap<>();
        User user = userRepository.getUserByUsername(username);
        boolean systemCheck = true;
        if(arRepository.existsById(uuid)){
            Activity activity = arRepository.getActivityById(uuid);

            if(activity instanceof Quiz){
                double score = 0;
                Quiz quiz = (Quiz)activity;
                List<Question> questions = quiz.getQuestions();
                for(Question q : questions){
                    if (!(q instanceof MultipleAnswer)){
                        systemCheck = false;
                    }
                }
                if (systemCheck){
                    for(Question q : questions) {
                        double questionScore = 0;
                        if (q instanceof MultipleAnswer) {
                            List<Choice> choiceList = ((MultipleAnswer) q).getChoiceList();
                            for (Choice c : choiceList) {
                                List<StudentChoice> studentChoiceList = c.getStudentChoices();
                                for (StudentChoice s : studentChoiceList) {
                                    if (s.getUser().equals(user)) {
                                        questionScore = Double.parseDouble(c.getCorrectness());
                                    }
                                }
                            }
                            score += questionScore;
                        }
                    }
                    String out = "" +  score / quiz.getQuestions().size();
                    List<StudentLongAnswer> studentLongAnswers=quiz.getStudentLongAnswers();
                    for(StudentLongAnswer s : studentLongAnswers){
                        if(s.getUser().equals(user)){
                            s.setGrade(""+score);

                            break;
                        }
                    }
                    res.put("obj", out);
                    res.put("status", true);
                    res.put("msg", "grade for quiz (assigned by system) returned successfully");
                    return res;
                }
                else{
                    List<StudentLongAnswer> studentLongAnswers=quiz.getStudentLongAnswers();
                    for(StudentLongAnswer s : studentLongAnswers){
                        if(s.getUser().equals(user)){
                            res.put("obj",s.getGrade());
                            res.put("status", true);
                            res.put("msg", "student grade assigned by ostad");
                            return res;
                        }
                    }
                }
            }
            else {
                List<StudentLongAnswer> studentLongAnswers=activity.getStudentLongAnswers();
                for(StudentLongAnswer s : studentLongAnswers){
                    if(s.getUser().equals(user)){
                        if(s.getGrade()==null){
                            res.put("obj","0");
                            res.put("status", true);
                            res.put("msg", "student grade assigned by ostad");
                            return res;
                        }
                        res.put("obj",s.getGrade());
                        res.put("status", true);
                        res.put("msg", "student grade assigned by ostad");
                        return res;
                    }
                }
                res.put("obj","0");
                res.put("status", true);
                res.put("msg", "student hasn't participated in activity");
                return res;
            }
        }
        res.put("status", false);
        res.put("msg", "activity/classroom does not exist");
        res.put("obj", null);
        return res;


    }

    public HashMap<String,Object> setUserGrade(String activityUUID, String username, String newGrade){
        HashMap<String, Object> res = new HashMap<>();
        User user = userRepository.getUserByUsername(username);

        if(arRepository.existsById(activityUUID)){
            Activity activity = arRepository.getActivityById(activityUUID);
            List<StudentLongAnswer> finalGrades = activity.getStudentLongAnswers();
            for(StudentLongAnswer grade : finalGrades){
                if(grade.getUser().equals(user)){
                    grade.setGrade(newGrade);
                    studentLongAnswerRepository.save(grade);
                    arRepository.save(activity);
                    res.put("msg", "successfully changed student grade");
                    res.put("status", true);
                    res.put("obj", null);
                    return res;
                }
            }
            // if we reach this line that means that that student doesn't have a final grade for this activity
            StudentLongAnswer newGradeDefinition = new StudentLongAnswer();
            newGradeDefinition.setGrade(newGrade);
            newGradeDefinition.setUser(user);
            newGradeDefinition.setActivity(activity);
            activity.getStudentLongAnswers().add(newGradeDefinition);
            user.getStudentLongAnswers().add(newGradeDefinition);
            userRepository.save(user);
            arRepository.save(activity);
            res.put("msg", "successfully changed student grade");
            res.put("status", true);
            res.put("obj", null);
            return res;
        }
        else {
            res.put("status", false);
            res.put("msg", "activity does not exist");
            res.put("obj", null);
            return res;
        }
    }

    public HashMap<String, Object> getARtype(String uuid){
        HashMap<String, Object> res =  new HashMap<>();
        if(arRepository.existsById(uuid)){
           Activity activity =  arRepository.getActivityById(uuid);
           if(activity instanceof Quiz){
               res.put("obj", "quiz");
           }
           /*
           else if(activity instanceof Assignment){
               res.put("obj", "assignment");
           }
           else if( activity instanceof Poll){
               res.put("obj", "poll");
           }
            */
            res.put("msg", "successfully returned activity type");
            res.put("status", true);
            return res;
        }
        res.put("obj", null);
        res.put("msg", "failed to fetch asssigment");
        res.put("status", false);
        return res;
    }

    public HashMap<String, Object> getARdata(String uuid){

        HashMap<String, Object> res =  new HashMap<>();
        if(arRepository.existsById(uuid)) {
            Activity activity = arRepository.getActivityById(uuid);
            HashMap<String, String> data = new HashMap<>();
            if (activity instanceof Quiz) {
                data.put("title", activity.getTitle());
                data.put("duration", ((Quiz) activity).getDuration());
                data.put("startDate", activity.getStartDate());
                data.put("endDate", activity.getEndDate());
                res.put("obj", data);
                res.put("msg", "quiz data returned successfully");
                res.put("status", true);
                return res;
            }
            /*
            else {
            }
             */
            res.put("obj", null);
            res.put("msg", "idk data returned successfully");
            res.put("status", true);
            return res;
        }
        res.put("obj", null);
        res.put("msg", "failed to find activity");
        res.put("status", false);
        return res;
    }


    public HashMap<String, Object> getQuizQuestions(String uuid){
        // returns a list of questions.
        // the list comes in a form of List<HashMap<QuestionUUID, OBJECT>>
        // object:
        // multiple answer: list<Hashmap<choiceUUID, text>>
        // long answer : string text

        HashMap<String, Object> res =  new HashMap<>();
        if(arRepository.existsById(uuid)) {
            Activity activity = arRepository.getActivityById(uuid);
            if (activity instanceof Quiz) {
                Quiz quiz = (Quiz) activity;
                List<Question> questions = quiz.getQuestions();
                List<HashMap<String, Object>> outQuestions = new ArrayList<>(); // Quuid, object
                for (Question q : questions) {
                    if (q instanceof MultipleAnswer) {
                        List<Choice> choiceList = ((MultipleAnswer) q).getChoiceList();
                        HashMap<String, Object> multipleQuestion = new HashMap<>();
                        if (choiceList.size() == 4) {

                            multipleQuestion.put("type", "multipleAnswer");
                            multipleQuestion.put("text", ((MultipleAnswer) q).getText());
                            multipleQuestion.put("uuid", q.getId());
                            List<HashMap<String, String>> choicesAndUUIDs = new ArrayList<>();
                            for (Choice c : choiceList) {
                                HashMap<String, String> choiceAndUUID = new HashMap<>();
                                choiceAndUUID.put("uuid", c.getId());
                                choiceAndUUID.put("text", c.getText());
                                choicesAndUUIDs.add(choiceAndUUID);
                            }
                            multipleQuestion.put("choices", choicesAndUUIDs);
                        } else if (choiceList.size() == 2) {

                            multipleQuestion.put("type", "trueFalse");
                            multipleQuestion.put("text", ((MultipleAnswer) q).getText());
                            multipleQuestion.put("uuid", q.getId());
                            List<HashMap<String, String>> choicesAndUUIDs = new ArrayList<>();
                            for (Choice c : choiceList) {
                                HashMap<String, String> choiceAndUUID = new HashMap<>();
                                choiceAndUUID.put("uuid", c.getId());
                                choiceAndUUID.put("text", c.getText());
                                choicesAndUUIDs.add(choiceAndUUID);
                            }
                            multipleQuestion.put("choices", choicesAndUUIDs);
                        }
                        outQuestions.add(multipleQuestion);
                    } else {
                        LongAnswer lq = (LongAnswer) q;
                        HashMap<String, Object> longAnswerQ = new HashMap<>();
                        longAnswerQ.put("type","longAnswer");
                        longAnswerQ.put("uuid", lq.getId());
                        longAnswerQ.put("text", lq.getText());
                        outQuestions.add(longAnswerQ);
                    }
                }
                res.put("obj", outQuestions);
                res.put("msg", "returned questions");
                res.put("status", true);
                return res;
            }
            res.put("obj", null);
            res.put("msg", "activity not quiz");
            res.put("status", false);
            return res;
        }
        res.put("obj",null);
        res.put("msg","failed to fetch activity. Acitivity uuid may not exist");
        res.put("status", false);
        return res;
    }

    public HashMap<String, Object> addQuiz(String classroomCode, String sectionUUID, HashMap<String,Object> quizData)
    {
        HashMap<String, Object> res = new HashMap<>();
        if(classroomRepository.existsByCode(classroomCode)){
            Classroom classroom = classroomRepository.getClassroomByCode(classroomCode);
            List<Section> sections = classroom.getSections();
            for(Section s : sections){
                if(s.getId().equals(sectionUUID)){
                    Quiz quiz = new Quiz();
                    quiz.setTitle((String) quizData.get("title"));
                    quiz.setStartDate((String) quizData.get("startDate"));
                    quiz.setEndDate((String) quizData.get("endDate"));
                    quiz.setDuration((String) quizData.get("duration"));
                    quiz.setDescription((String) quizData.get("description"));
                    quiz.setSection(s);
                    List<Question> quizQuestionList = new ArrayList<>();
                    List<HashMap<String, Object>> quizQuestions = (List<HashMap<String, Object>>) quizData.get("questions");
                    for(HashMap<String,Object> questionHashMap : quizQuestions){
                        if (questionHashMap.get("questionType").equals("multipleAnswer")){
                            MultipleAnswer multipleAnswer = new MultipleAnswer();
                            multipleAnswer.setText((String) questionHashMap.get("questionText"));
                            List<Choice> choiceList = new ArrayList<>();
                            HashMap<String, String> choiceHashMap = (HashMap<String, String>) questionHashMap.get("choices");
                            Choice correctChoice = new Choice();
                            correctChoice.setText(choiceHashMap.get("true"));
                            correctChoice.setCorrectness("100");
                            correctChoice.setMultipleAnswer(multipleAnswer);

                            Choice false1 = new Choice();
                            false1.setText(choiceHashMap.get("false1"));
                            false1.setCorrectness("0");
                            false1.setMultipleAnswer(multipleAnswer);
                            System.out.println("\n\n\n\n\n\n"+false1.getText());

                            Choice false2 = new Choice();
                            false2.setText(choiceHashMap.get("false2"));
                            false2.setCorrectness("0");
                            false2.setMultipleAnswer(multipleAnswer);

                            Choice false3 = new Choice();
                            false3.setText(choiceHashMap.get("false3"));
                            false3.setCorrectness("0");
                            false3.setMultipleAnswer(multipleAnswer);

                            choiceList.add(correctChoice);
                            choiceList.add(false1);
                            choiceList.add(false2);
                            choiceList.add(false3);
                            multipleAnswer.setChoiceList(choiceList);
                            multipleAnswer.setQuiz(quiz);

                            quizQuestionList.add(multipleAnswer);
                            //questionRepository.save(multipleAnswer);
                        }
                        else if (questionHashMap.get("questionType").equals("trueFalse")){
                            MultipleAnswer multipleAnswer = new MultipleAnswer();
                            multipleAnswer.setText((String) questionHashMap.get("questionText"));
                            List<Choice> choiceList = new ArrayList<>();
                            HashMap<String, String> choiceHashMap = (HashMap<String, String>) questionHashMap.get("choices");
                            Choice correctChoice = new Choice();
                            correctChoice.setText(choiceHashMap.get("true"));
                            correctChoice.setCorrectness("100");
                            correctChoice.setMultipleAnswer(multipleAnswer);

                            Choice false1 = new Choice();
                            false1.setText(choiceHashMap.get("false1"));
                            false1.setCorrectness("0");
                            false1.setMultipleAnswer(multipleAnswer);
                            System.out.println("\n\n\n\n\n\n"+false1.getText());

                            choiceList.add(correctChoice);
                            choiceList.add(false1);
                            multipleAnswer.setChoiceList(choiceList);
                            System.out.println("\n\n\n\nhereerererer"+ multipleAnswer.getChoiceList().get(0).getText()+"\n\n\n\n");
                            multipleAnswer.setQuiz(quiz);

                            quizQuestionList.add(multipleAnswer);
                            //questionRepository.save(multipleAnswer);
                        }
                        else if(questionHashMap.get("questionType").equals("longAnswer")){
                            LongAnswer longAnswer  = new LongAnswer();
                            longAnswer.setText((String) questionHashMap.get("questionText"));
                            longAnswer.setQuiz(quiz);
                            quizQuestionList.add(longAnswer);
                            //questionRepository.save(longAnswer);
                        }
                    }
                    quiz.setQuestions(quizQuestionList);
                    arRepository.save(quiz);
                    s.getActivities().add(quiz);
                    sectionRepository.save(s);
                    res.put("msg", "new quiz added to section successfully");
                    res.put("status", true);
                    res.put("obj", null);
                 //   System.out.println("kirrr"+((MultipleAnswer)((Quiz)arRepository.getActivityById(quiz.getId())).getQuestions().get(0)).getChoiceList().get(0).getText());
                    return res;
                }
            }
            res.put("msg", "section does not exist within classroom");
            res.put("status", false);
            res.put("obj", null);
            return res;
        }
        res.put("msg", "classroom does not exist");
        res.put("status", false);
        res.put("obj", null);
        return res;
    }


    public HashMap<String, Object> putUserAnswers(String username, String activityUUID, List<HashMap<String,String>> choiceUUIDs){
        HashMap<String, Object> res = new HashMap<>();
        User user = userRepository.getUserByUsername(username);
        for(HashMap<String, String> answerHashMap : choiceUUIDs) {
            if(choiceRepository.existsById(answerHashMap.get("uuid"))) {
                Choice choice = choiceRepository.getChoiceById(answerHashMap.get("uuid"));
                StudentChoice studentChoice = new StudentChoice();
                studentChoice.setChoice(choice);
                studentChoice.setUser(user);
                user.getStudentChoices().add(studentChoice);
                choice.getStudentChoices().add(studentChoice);
                choiceRepository.save(choice);
            }
            else {
                if(longAnswerRepository.existsById(answerHashMap.get("uuid"))){
                    LongAnswer longAnswer = longAnswerRepository.getLongAnswerById(answerHashMap.get("uuid"));
                    StudentLongAnswer studentLongAnswer = new StudentLongAnswer();
                    studentLongAnswer.setLongAnswer(longAnswer);
                    studentLongAnswer.setUser(user);
                    studentLongAnswer.setStudentAnswer(answerHashMap.get("answer"));
                    user.getStudentLongAnswers().add(studentLongAnswer);
                    longAnswer.getStudentLongAnswers().add(studentLongAnswer);
                    studentLongAnswerRepository.save(studentLongAnswer);

                }
                else {
                    res.put("status", false);
                    res.put("msg", "invalid choice uuid");
                    res.put("obj", null);
                    return res;
                }
            }
        }
        StudentLongAnswer studentLongAnswer = new StudentLongAnswer();
        studentLongAnswer.setUser(user);
        studentLongAnswer.setActivity(arRepository.getActivityById(activityUUID));
        studentLongAnswer.setGrade("0");
        user.getStudentLongAnswers().add(studentLongAnswer);
        userRepository.save(user);
        res.put("status", true);
        res.put("msg", "successfully registered user grades");
        res.put("obj", null);
        return res;
    }

    public HashMap<String, Object> getAnsweredQuiz(String username, String uuid){
        // returns a list of questions.
        // the list comes in a form of List<HashMap<QuestionUUID, OBJECT>>
        // object:
        // multiple answer: list<Hashmap<choiceUUID, text>>
        // long answer : string text

        User user = userRepository.getUserByUsername(username);

        HashMap<String, Object> res =  new HashMap<>();
        if(arRepository.existsById(uuid)) {
            Activity activity = arRepository.getActivityById(uuid);
            if (activity instanceof Quiz) {
                Quiz quiz = (Quiz) activity;
                List<Question> questions = quiz.getQuestions();
                List<HashMap<String, Object>> outQuestions = new ArrayList<>(); // Quuid, object
                for (Question q : questions) {
                    if (q instanceof MultipleAnswer) {
                        List<Choice> choiceList = ((MultipleAnswer) q).getChoiceList();
                        HashMap<String, Object> multipleQuestion = new HashMap<>();
                        if (choiceList.size() == 4) {

                            multipleQuestion.put("type", "multipleAnswer");
                            multipleQuestion.put("text", ((MultipleAnswer) q).getText());
                            multipleQuestion.put("uuid", q.getId());
                            List<HashMap<String, String>> choicesAndUUIDs = new ArrayList<>();
                            for (Choice c : choiceList) {
                                HashMap<String, String> choiceAndUUID = new HashMap<>();
                                choiceAndUUID.put("uuid", c.getId());
                                choiceAndUUID.put("text", c.getText());

                                List<StudentChoice> choiceList1 = c.getStudentChoices();
                                for(StudentChoice studentChoice : choiceList1){
                                    if (studentChoice.getUser().equals(user)){
                                        choiceAndUUID.put("stdAnswer", studentChoice.getChoice().getText());
                                    }
                                }
                                choicesAndUUIDs.add(choiceAndUUID);
                            }
                            multipleQuestion.put("choices", choicesAndUUIDs);
                        } else if (choiceList.size() == 2) {

                            multipleQuestion.put("type", "trueFalse");
                            multipleQuestion.put("text", ((MultipleAnswer) q).getText());
                            multipleQuestion.put("uuid", q.getId());
                            List<HashMap<String, String>> choicesAndUUIDs = new ArrayList<>();
                            for (Choice c : choiceList) {
                                HashMap<String, String> choiceAndUUID = new HashMap<>();
                                choiceAndUUID.put("uuid", c.getId());
                                choiceAndUUID.put("text", c.getText());
                                choicesAndUUIDs.add(choiceAndUUID);

                                List<StudentChoice> choiceList1 = c.getStudentChoices();
                                for(StudentChoice studentChoice : choiceList1){
                                    if (studentChoice.getUser().equals(user)){
                                        choiceAndUUID.put("stdAnswer", studentChoice.getChoice().getText());
                                    }
                                }
                            }

                            multipleQuestion.put("choices", choicesAndUUIDs);
                        }
                        outQuestions.add(multipleQuestion);
                    } else {
                        LongAnswer lq = (LongAnswer) q;
                        HashMap<String, Object> longAnswerQ = new HashMap<>();
                        longAnswerQ.put("type","longAnswer");
                        longAnswerQ.put("uuid", lq.getId());
                        longAnswerQ.put("text", lq.getText());
                        List<StudentLongAnswer> choiceList1 = lq.getStudentLongAnswers();
                        for(StudentLongAnswer studentChoice : choiceList1){
                            if (studentChoice.getUser().equals(user)){
                                longAnswerQ.put("stdAnswer", studentChoice.getStudentAnswer());
                            }
                        }
                        outQuestions.add(longAnswerQ);
                    }
                }
                res.put("obj", outQuestions);
                res.put("msg", "returned questions");
                res.put("status", true);
                return res;
            }
            res.put("obj", null);
            res.put("msg", "activity not quiz");
            res.put("status", false);
            return res;
        }
        res.put("obj",null);
        res.put("msg","failed to fetch activity. Acitivity uuid may not exist");
        res.put("status", false);
        return res;
    }

}
