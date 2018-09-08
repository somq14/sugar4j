package jp.ac.meiji.igusso.scheduling.ikegami;

import lombok.Getter;
import lombok.NonNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

final class Xml2ProblemConverter {
  private Document document;
  private long beginDate;
  private long endDate;
  private int length;
  private int overStaffingWeight;
  private int underStaffingWeight;
  private List<Shift> shifts;
  private List<ShiftGroup> shiftGroups;
  private List<Skill> skills;
  private List<SkillGroup> skillGroups;
  private List<Cover> covers;
  private List<Contract> contracts;
  private List<Staff> staffs;
  private List<DayOffRequest> dayOffRequests;
  private List<ShiftOffRequest> shiftOffRequests;
  private List<ShiftOnRequest> shiftOnRequests;
  private List<FixedAssignment> fixedAssignments;

  Xml2ProblemConverter() {}

  // YYYY-MM-DD
  private static long parseDate(String date) {
    String[] values = date.split("-");
    int year = Integer.valueOf(values[0]);
    int month = Integer.valueOf(values[1]) - 1;
    int day = Integer.valueOf(values[2]);

    Calendar cal = Calendar.getInstance();
    cal.set(year, month, day, 0, 0, 0);
    return cal.getTimeInMillis() / 1000L * 1000L;
  }

  // HH:MM:SS or HH:MM
  private static long parseTimeOfDay(String timeOfDay) {
    String[] values = timeOfDay.split(":");
    int hour = Integer.valueOf(values[0]);
    int minute = Integer.valueOf(values[1]);
    int second = values.length == 3 ? Integer.valueOf(values[2]) : 0;
    return (3600 * hour + 60 * minute + second) * 1000L;
  }

  private static int parseDayOfWeek(String dayOfWeek) {
    switch (dayOfWeek) {
      case "Sunday":
        return 0;
      case "Monday":
        return 1;
      case "Tuesday":
        return 2;
      case "Wednesday":
        return 3;
      case "Thursday":
        return 4;
      case "Friday":
        return 5;
      case "Saturday":
        return 6;
      default:
        throw new RuntimeException("Unknown Day Of Week : " + dayOfWeek);
    }
  }

  private Document parseXml(InputStream instance) throws IOException, SAXException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

    DocumentBuilder documentBuilder = null;
    try {
      documentBuilder = factory.newDocumentBuilder();
    } catch (ParserConfigurationException ex) {
      ex.printStackTrace();
      throw new RuntimeException(ex);
    }

    Document document = null;
    try {
      document = documentBuilder.parse(instance);
    } catch (IOException ex) {
      throw ex;
    } catch (SAXException ex) {
      throw ex;
    }

    return document;
  }

  private void validateXml() throws IOException {
    URL schemaUrl = null;
    try {
      schemaUrl = new URL(
          "file", "localhost", "/home/k_matsuura/coptool/instances/SchedulingPeriod-3.0.xsd");
      // new URL("https", "www.staffrostersolutions.com", "/support/SchedulingPeriod-3.0.xsd");
    } catch (MalformedURLException ex) {
      ex.printStackTrace();
      throw new RuntimeException(ex);
    }

    SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    Schema schema = null;
    try {
      schema = factory.newSchema(schemaUrl);
    } catch (SAXException ex) {
      ex.printStackTrace();
      throw new RuntimeException(ex);
    }

    Validator validator = schema.newValidator();
    try {
      validator.validate(new DOMSource(document));
    } catch (SAXException ex) {
      ex.printStackTrace();
    } catch (IOException ex) {
      throw ex;
    }
  }

  private List<Element> getChildrenByTagName(Element root, String tagName) {
    List<Element> res = new ArrayList<>();

    for (Node it = root.getFirstChild(); it != null; it = it.getNextSibling()) {
      if (it.getNodeType() != Node.ELEMENT_NODE) {
        continue;
      }

      Element elm = (Element) it;
      if (elm.getTagName().equals(tagName)) {
        res.add(elm);
      }
    }

    return Collections.unmodifiableList(res);
  }

  private Element getChildByTagName(Element root, String tagName) {
    for (Node it = root.getFirstChild(); it != null; it = it.getNextSibling()) {
      if (it.getNodeType() != Node.ELEMENT_NODE) {
        continue;
      }

      Element elm = (Element) it;
      if (elm.getTagName().equals(tagName)) {
        return elm;
      }
    }

    return null;
  }

  private List<Element> getChildren(Element root) {
    List<Element> res = new ArrayList<>();

    for (Node it = root.getFirstChild(); it != null; it = it.getNextSibling()) {
      if (it.getNodeType() != Node.ELEMENT_NODE) {
        continue;
      }
      Element elm = (Element) it;
      res.add(elm);
    }

    return res;
  }

  private long readBeginDate() {
    Element elm = getChildByTagName(document.getDocumentElement(), "StartDate");
    return parseDate(elm.getTextContent());
  }

  private long readEndDate() {
    Element elm = getChildByTagName(document.getDocumentElement(), "EndDate");
    return parseDate(elm.getTextContent());
  }

  private int readOverStaffingWeight() {
    Element coverWeightsElm = getChildByTagName(document.getDocumentElement(), "CoverWeights");
    if (coverWeightsElm == null) {
      return -1;
    }
    Element maxOverStaffingElm = getChildByTagName(coverWeightsElm, "MaxOverStaffing");
    if (maxOverStaffingElm == null) {
      return -1;
    }
    return Integer.valueOf(maxOverStaffingElm.getTextContent());
  }

  private int readUnderStaffingWeight() {
    Element coverWeightsElm = getChildByTagName(document.getDocumentElement(), "CoverWeights");
    if (coverWeightsElm == null) {
      return -1;
    }
    Element minUnderStaffingElm = getChildByTagName(coverWeightsElm, "MinUnderStaffing");
    if (minUnderStaffingElm == null) {
      return -1;
    }
    return Integer.valueOf(minUnderStaffingElm.getTextContent());
  }

  private List<Shift> readShiftTypes() {
    List<Shift> res = new ArrayList<>();

    Element shiftTypesElm = getChildByTagName(document.getDocumentElement(), "ShiftTypes");
    for (Element shiftElm : getChildren(shiftTypesElm)) {
      String id = shiftElm.getAttribute("ID");

      Element beginTimeElm = getChildByTagName(shiftElm, "StartTime");
      final long beginTime =
          beginTimeElm == null ? 0L : parseTimeOfDay(beginTimeElm.getTextContent());

      Element endTimeElm = getChildByTagName(shiftElm, "EndTime");
      long endTime = endTimeElm == null ? 0L : parseTimeOfDay(endTimeElm.getTextContent());

      Element durationElm = getChildByTagName(shiftElm, "Duration");
      if (durationElm != null) {
        endTime = beginTime + Integer.valueOf(durationElm.getTextContent());
      }

      Element nameElm = getChildByTagName(shiftElm, "Name");
      final String name = nameElm == null ? id : nameElm.getTextContent();

      Element labelElm = getChildByTagName(shiftElm, "Label");
      final String label = labelElm == null ? id : labelElm.getTextContent();

      Element colorElm = getChildByTagName(shiftElm, "Color");
      final String color = colorElm == null ? "LightBlue" : colorElm.getTextContent();

      Element autoAllocateElm = getChildByTagName(shiftElm, "AutoAllocate");
      final boolean autoAllocate =
          autoAllocateElm == null ? true : Boolean.valueOf(autoAllocateElm.getTextContent());

      Element timeUnitsElm = getChildByTagName(shiftElm, "TimeUnits");
      if (timeUnitsElm != null) {
        throw new RuntimeException("TimeUnits Tag Are Not Supported");
      }

      Element timePeriodsElm = getChildByTagName(shiftElm, "TimePeriods");
      if (timePeriodsElm != null) {
        throw new RuntimeException("TimePeriods Tag Are Not Supported");
      }

      Element resourcesElm = getChildByTagName(shiftElm, "Resources");
      if (resourcesElm != null) {
        throw new RuntimeException("Resources Tag Are Not Supported");
      }

      res.add(new Shift(id, label, color, name, beginTime, endTime, autoAllocate));
    }

    return Collections.unmodifiableList(res);
  }

  private Shift getShiftById(String id) {
    for (Shift s : shifts) {
      if (s.getId().equals(id)) {
        return s;
      }
    }
    return null;
  }

  private List<ShiftGroup> readShiftGroups() {
    List<ShiftGroup> res = new ArrayList<>();

    Element groupsElm = getChildByTagName(document.getDocumentElement(), "ShiftGroups");
    for (Element groupElm : getChildren(groupsElm)) {
      String id = groupElm.getAttribute("ID");

      List<Shift> members = new ArrayList<>();
      for (Element shiftElm : getChildrenByTagName(groupElm, "Shift")) {
        members.add(getShiftById(shiftElm.getTextContent()));
      }

      res.add(new ShiftGroup(id, members));
    }

    return Collections.unmodifiableList(res);
  }

  private ShiftGroup getShiftGroupById(String id) {
    for (ShiftGroup s : shiftGroups) {
      if (s.getId().equals(id)) {
        return s;
      }
    }
    return null;
  }

  private List<Skill> readSkills() {
    List<Skill> res = new ArrayList<>();

    Element skillsElm = getChildByTagName(document.getDocumentElement(), "Skills");
    for (Element skillElm : getChildren(skillsElm)) {
      String id = skillElm.getAttribute("ID");
      String label = getChildByTagName(skillElm, "Label").getTextContent();
      res.add(new Skill(id, label));
    }

    return Collections.unmodifiableList(res);
  }

  private Skill getSkillById(String id) {
    for (Skill s : skills) {
      if (s.getId().equals(id)) {
        return s;
      }
    }
    return null;
  }

  private List<SkillGroup> readSkillGroups() {
    List<SkillGroup> res = new ArrayList<>();

    Element groupsElm = getChildByTagName(document.getDocumentElement(), "SkillGroups");
    for (Element groupElm : getChildren(groupsElm)) {
      String id = groupElm.getAttribute("ID");

      List<Skill> members = new ArrayList<>();
      for (Element skillElm : getChildrenByTagName(groupElm, "Skill")) {
        members.add(getSkillById(skillElm.getTextContent()));
      }
      res.add(new SkillGroup(id, members));
    }

    return Collections.unmodifiableList(res);
  }

  private SkillGroup getSkillGroupById(String id) {
    for (SkillGroup s : skillGroups) {
      if (s.getId().equals(id)) {
        return s;
      }
    }
    return null;
  }

  private List<Cover> readCovers() {
    List<Cover> res = new ArrayList<>();

    Element coverReqsElm = getChildByTagName(document.getDocumentElement(), "CoverRequirements");
    for (Element coverReqElm : getChildren(coverReqsElm)) {
      if (coverReqElm.getTagName().equals("DayOfWeekCover")) {
        throw new RuntimeException("DayOfWeekCover Tag Are Not Supported");
      }

      // DateSpecificCover
      int day = 0;
      Element dayElm = getChildByTagName(coverReqElm, "Day");
      if (dayElm != null) {
        day = Integer.valueOf(dayElm.getTextContent());
      }

      Element dateElm = getChildByTagName(coverReqElm, "Date");
      if (dateElm != null) {
        day = (int) ((parseDate(dateElm.getTextContent()) - beginDate) / (24L * 3600L * 1000L));
      }

      for (Element coverElm : getChildrenByTagName(coverReqElm, "Cover")) {
        int min = -1;
        int max = -1;
        Skill skill = null;
        SkillGroup skillGroup = null;
        Shift shift = null;
        ShiftGroup shiftGroup = null;
        String label = "";

        for (Element elm : getChildren(coverElm)) {
          switch (elm.getTagName()) {
            case "Min": {
              min = Integer.valueOf(elm.getTextContent());
            } break;
            case "Max": {
              max = Integer.valueOf(elm.getTextContent());
            } break;
            case "Skill": {
              skill = getSkillById(elm.getTextContent());
            } break;
            case "SkillGroup": {
              skillGroup = getSkillGroupById(elm.getTextContent());
            } break;
            case "Shift": {
              shift = getShiftById(elm.getTextContent());
            } break;
            case "ShiftGroup": {
              shiftGroup = getShiftGroupById(elm.getTextContent());
            } break;
            case "Label": {
              label = elm.getTextContent();
            } break;
            default:
              throw new RuntimeException("Unknown Tag : " + elm.getTagName());
          }
        }

        res.add(new Cover(day, min, max, skill, skillGroup, shift, shiftGroup, label));
      }
    }

    return Collections.unmodifiableList(res);
  }

  private Pattern readPattern(@NonNull Element patternElm) {
    int startDay = -1;
    Element startElm = getChildByTagName(patternElm, "Start");
    if (startElm != null) {
      startDay = Integer.valueOf(startElm.getTextContent());
    }
    Element startDateElm = getChildByTagName(patternElm, "StartDate");
    if (startDateElm != null) {
      startDay =
          (int) ((parseDate(startDateElm.getTextContent()) - beginDate) / (24L * 3600L * 1000L));
    }

    int startDayOfWeek = -1;
    Element startDayElm = getChildByTagName(patternElm, "StartDay");
    if (startDayElm != null) {
      startDayOfWeek = parseDayOfWeek(startDayElm.getTextContent());
    }

    if (getChildByTagName(patternElm, "Starts") != null) {
      throw new RuntimeException("Starts tag is not supported");
    }

    if (getChildByTagName(patternElm, "StartExcludes") != null) {
      throw new RuntimeException("StartExcludes tag is not supported");
    }

    List<PatternAtom> atoms = new ArrayList<>();
    for (Element elm : getChildren(patternElm)) {
      switch (elm.getTagName()) {
        case "Shift": {
          switch (elm.getTextContent()) {
            case "$": {
              atoms.add(new PatternAtom(PatternAtom.Type.ANY_SHIFT));
            } break;
            case "-": {
              atoms.add(new PatternAtom(PatternAtom.Type.DAY_OFF));
            } break;
            case "*": {
              atoms.add(new PatternAtom(PatternAtom.Type.ANY));
            } break;
            default:
              atoms.add(
                  new PatternAtom(PatternAtom.Type.SHIFT, getShiftById(elm.getTextContent())));
          }
        } break;
        case "ShiftGroup": {
          atoms.add(
              new PatternAtom(PatternAtom.Type.GROUP, getShiftGroupById(elm.getTextContent())));
        } break;
        case "NotShift": {
          atoms.add(
              new PatternAtom(PatternAtom.Type.NOT_SHIFT, getShiftById(elm.getTextContent())));
        } break;
        case "NotGroup": {
          atoms.add(
              new PatternAtom(PatternAtom.Type.NOT_GROUP, getShiftGroupById(elm.getTextContent())));
        } break;
        default:;
      }
    }

    return new Pattern(startDay, startDayOfWeek, atoms);
  }

  private List<Contract> readContracts() {
    List<Contract> res = new ArrayList<>();

    Element contractsElm = getChildByTagName(document.getDocumentElement(), "Contracts");
    for (Element contractElm : getChildren(contractsElm)) {
      String id = contractElm.getAttribute("ID");

      Element labelElm = getChildByTagName(contractElm, "Label");
      String label = labelElm == null ? id : labelElm.getTextContent();

      List<PatternContract> patternContracts = new ArrayList<>();

      for (Element patternsElm : getChildrenByTagName(contractElm, "Patterns")) {
        for (Element matchElm : getChildrenByTagName(patternsElm, "Match")) {
          int beginDay = 0;
          Element regionStartElm = getChildByTagName(matchElm, "RegionStart");
          if (regionStartElm != null) {
            beginDay = Integer.valueOf(regionStartElm.getTextContent());
          }
          Element regionStartDateElm = getChildByTagName(matchElm, "RegionStartDate");
          if (regionStartDateElm != null) {
            beginDay = (int) ((parseDate(regionStartDateElm.getTextContent()) - beginDate)
                / (24L * 3600L * 1000L));
          }

          int endDay = length - 1;
          Element regionEndElm = getChildByTagName(matchElm, "RegionEnd");
          if (regionEndElm != null) {
            endDay = Integer.valueOf(regionEndElm.getTextContent());
          }
          Element regionEndDateElm = getChildByTagName(matchElm, "RegionEndDate");
          if (regionEndDateElm != null) {
            endDay = (int) ((parseDate(regionEndDateElm.getTextContent()) - beginDate)
                / (24L * 3600L * 1000L));
          }

          Element minElm = getChildByTagName(matchElm, "Min");
          int min = -1;
          int minWeight = 0;
          String minLabel = "";

          if (minElm != null) {
            min = Integer.valueOf(getChildByTagName(minElm, "Count").getTextContent());

            Element minWeightElm = getChildByTagName(minElm, "Weight");
            if (minWeightElm != null) {
              minWeight = Integer.valueOf(minWeightElm.getTextContent());
              if (!minWeightElm.getAttribute("function").equals("")) {
                throw new RuntimeException("function attribute is not supported");
              }
            }

            Element minLabelElm = getChildByTagName(minElm, "Label");
            if (minLabelElm != null) {
              minLabel = minLabelElm.getTextContent();
            }

            if (getChildByTagName(minElm, "Var") != null) {
              throw new RuntimeException("Var tag is not supported");
            }
          }

          Element maxElm = getChildByTagName(matchElm, "Max");
          int max = -1;
          int maxWeight = 0;
          String maxLabel = "";

          if (maxElm != null) {
            max = Integer.valueOf(getChildByTagName(maxElm, "Count").getTextContent());

            Element maxWeightElm = getChildByTagName(maxElm, "Weight");
            if (maxWeightElm != null) {
              maxWeight = Integer.valueOf(maxWeightElm.getTextContent());
              if (!maxWeightElm.getAttribute("function").equals("")) {
                throw new RuntimeException("function attribute is not supported");
              }
            }

            Element maxLabelElm = getChildByTagName(maxElm, "Label");
            if (maxLabelElm != null) {
              maxLabel = maxLabelElm.getTextContent();
            }

            if (getChildByTagName(maxElm, "Var") != null) {
              throw new RuntimeException("Var tag is not supported");
            }
          }

          List<Pattern> patterns = new ArrayList<>();
          for (Element patternElm : getChildrenByTagName(matchElm, "Pattern")) {
            patterns.add(readPattern(patternElm));
          }

          patternContracts.add(new PatternContract(
              beginDay, endDay, min, minWeight, minLabel, max, maxWeight, maxLabel, patterns));
        }
      }
      res.add(new Contract(id, label, patternContracts));
    }

    return Collections.unmodifiableList(res);
  }

  private Contract getContractById(String id) {
    for (Contract c : contracts) {
      if (c.getId().equals(id)) {
        return c;
      }
    }
    return null;
  }

  private List<Staff> readStaffs() {
    List<Staff> res = new ArrayList<>();

    Element employeesElm = getChildByTagName(document.getDocumentElement(), "Employees");
    for (Element employeeElm : getChildrenByTagName(employeesElm, "Employee")) {
      String id = employeeElm.getAttribute("ID");

      String name = id;
      Element nameElm = getChildByTagName(employeeElm, "Name");
      if (nameElm != null) {
        name = nameElm.getTextContent();
      }

      List<Contract> contracts = new ArrayList<>();
      for (Element contractElm : getChildrenByTagName(employeeElm, "ContractID")) {
        contracts.add(getContractById(contractElm.getTextContent()));
      }

      List<Skill> skills = new ArrayList<>();
      Element skillsElm = getChildByTagName(employeeElm, "Skills");
      for (Element skillElm : getChildrenByTagName(skillsElm, "Skill")) {
        skills.add(getSkillById(skillElm.getTextContent()));
      }

      if (getChildByTagName(employeesElm, "CoverResources") != null) {
        throw new RuntimeException("CoverRequirements tag is not supported");
      }

      res.add(new Staff(id, name, skills, contracts));
    }
    return Collections.unmodifiableList(res);
  }

  private Staff getStaffById(String id) {
    for (Staff s : staffs) {
      if (s.getId().equals(id)) {
        return s;
      }
    }
    return null;
  }

  private List<DayOffRequest> readDayOffRequests() {
    List<DayOffRequest> res = new ArrayList<>();

    Element dayOffRequestsElm = getChildByTagName(document.getDocumentElement(), "DayOffRequests");
    for (Element dayOffRequestElm : getChildrenByTagName(dayOffRequestsElm, "DayOff")) {
      int weight = -1;
      if (!dayOffRequestElm.getAttribute("weight").equals("")) {
        weight = Integer.valueOf(dayOffRequestElm.getAttribute("weight"));
      }

      Element employeeIdElm = getChildByTagName(dayOffRequestElm, "EmployeeID");
      Staff staff = getStaffById(employeeIdElm.getTextContent());

      int day = -1;
      Element dateElm = getChildByTagName(dayOffRequestElm, "Date");
      if (dateElm != null) {
        day = (int) ((parseDate(dateElm.getTextContent()) - beginDate) / (24L * 3600L * 1000L));
      }

      Element dayElm = getChildByTagName(dayOffRequestElm, "Day");
      if (dayElm != null) {
        day = Integer.valueOf(dayElm.getTextContent());
      }

      res.add(new DayOffRequest(staff, day, weight));
    }

    return Collections.unmodifiableList(res);
  }

  private List<ShiftOffRequest> readShiftOffRequests() {
    List<ShiftOffRequest> res = new ArrayList<>();

    Element shiftOffRequestsElm =
        getChildByTagName(document.getDocumentElement(), "ShiftOffRequests");
    for (Element shiftOffRequestElm : getChildrenByTagName(shiftOffRequestsElm, "ShiftOff")) {
      int weight = -1;
      if (!shiftOffRequestElm.getAttribute("weight").equals("")) {
        weight = Integer.valueOf(shiftOffRequestElm.getAttribute("weight"));
      }

      Element shiftElm = getChildByTagName(shiftOffRequestElm, "ShiftTypeID");
      Shift shift = getShiftById(shiftElm.getTextContent());

      Element employeeIdElm = getChildByTagName(shiftOffRequestElm, "EmployeeID");
      Staff staff = getStaffById(employeeIdElm.getTextContent());

      int day = -1;
      Element dateElm = getChildByTagName(shiftOffRequestElm, "Date");
      if (dateElm != null) {
        day = (int) ((parseDate(dateElm.getTextContent()) - beginDate) / (24L * 3600L * 1000L));
      }

      Element dayElm = getChildByTagName(shiftOffRequestElm, "Day");
      if (dayElm != null) {
        day = Integer.valueOf(dayElm.getTextContent());
      }

      res.add(new ShiftOffRequest(staff, shift, day, weight));
    }

    return Collections.unmodifiableList(new ArrayList<>(res));
  }

  private List<ShiftOnRequest> readShiftOnRequests() {
    List<ShiftOnRequest> res = new ArrayList<>();

    Element shiftOnRequestsElm =
        getChildByTagName(document.getDocumentElement(), "ShiftOnRequests");
    for (Element shiftOnRequestElm : getChildrenByTagName(shiftOnRequestsElm, "ShiftOn")) {
      int weight = -1;
      if (!shiftOnRequestElm.getAttribute("weight").equals("")) {
        weight = Integer.valueOf(shiftOnRequestElm.getAttribute("weight"));
      }

      Element shiftElm = getChildByTagName(shiftOnRequestElm, "ShiftTypeID");
      Shift shift = null;
      if (shiftElm != null) {
        shift = getShiftById(shiftElm.getTextContent());
      }

      Element shiftGroupIdElm = getChildByTagName(shiftOnRequestElm, "ShiftGroupID");
      ShiftGroup shiftGroup = null;
      if (shiftGroupIdElm != null) {
        shiftGroup = getShiftGroupById(shiftGroupIdElm.getTextContent());
      }

      Element shiftGroupElm = getChildByTagName(shiftOnRequestElm, "ShiftGroup");
      if (shiftGroupElm != null) {
        throw new RuntimeException("ShiftGroup tag is no supported");
      }

      Element employeeIdElm = getChildByTagName(shiftOnRequestElm, "EmployeeID");
      Staff staff = getStaffById(employeeIdElm.getTextContent());

      int day = -1;
      Element dateElm = getChildByTagName(shiftOnRequestElm, "Date");
      if (dateElm != null) {
        day = (int) ((parseDate(dateElm.getTextContent()) - beginDate) / (24L * 3600L * 1000L));
      }

      Element dayElm = getChildByTagName(shiftOnRequestElm, "Day");
      if (dayElm != null) {
        day = Integer.valueOf(dayElm.getTextContent());
      }

      res.add(new ShiftOnRequest(staff, shift, shiftGroup, day, weight));
    }

    return Collections.unmodifiableList(new ArrayList<>(res));
  }

  private List<FixedAssignment> readFixedAssignments() {
    List<FixedAssignment> res = new ArrayList<>();

    Element fixedAssignmentsElm =
        getChildByTagName(document.getDocumentElement(), "FixedAssignments");
    for (Element employeeElm : getChildrenByTagName(fixedAssignmentsElm, "Employee")) {
      Staff staff = getStaffById(getChildByTagName(employeeElm, "EmployeeID").getTextContent());
      for (Element assignElm : getChildrenByTagName(employeeElm, "Assign")) {
        Shift shift = getShiftById(getChildByTagName(assignElm, "Shift").getTextContent());

        int day = -1;
        Element dateElm = getChildByTagName(assignElm, "Date");
        if (dateElm != null) {
          day = (int) ((parseDate(dateElm.getTextContent()) - beginDate) / (24L * 3600L * 1000L));
        }

        Element dayElm = getChildByTagName(assignElm, "Day");
        if (dayElm != null) {
          day = Integer.valueOf(dayElm.getTextContent());
        }

        res.add(new FixedAssignment(staff, shift, day));
      }
    }

    return Collections.unmodifiableList(res);
  }

  Problem convert(@NonNull File instance) throws FileNotFoundException, IOException, SAXException {
    return convert(new FileInputStream(instance));
  }

  Problem convert(@NonNull InputStream instance)
      throws FileNotFoundException, IOException, SAXException {
    this.document = parseXml(instance);
    validateXml();

    this.beginDate = readBeginDate();
    this.endDate = readEndDate();
    this.length = (int) (1L + (endDate - beginDate) / (24L * 3600L * 1000L));
    this.overStaffingWeight = readOverStaffingWeight();
    this.underStaffingWeight = readUnderStaffingWeight();

    this.shifts = readShiftTypes();
    this.shiftGroups = readShiftGroups();
    this.skills = readSkills();
    this.skillGroups = readSkillGroups();
    this.covers = readCovers();
    this.contracts = readContracts();
    this.staffs = readStaffs();
    this.dayOffRequests = readDayOffRequests();
    this.shiftOffRequests = readShiftOffRequests();
    this.shiftOnRequests = readShiftOnRequests();
    this.fixedAssignments = readFixedAssignments();

    return new ProblemImpl(beginDate, endDate, length, overStaffingWeight, underStaffingWeight,
        staffs, shifts, shiftGroups, skills, skillGroups, contracts, covers, dayOffRequests,
        shiftOffRequests, shiftOnRequests, fixedAssignments);
  }

  public static void main(String[] args) throws Exception {
    // while (true)
    Problem problem = new Xml2ProblemConverter().convert(new File(args[0]));
    for (Skill e : problem.getSkills()) {
      System.out.println(e);
    }
    System.out.println();
    for (SkillGroup e : problem.getSkillGroups()) {
      System.out.println(e);
    }
    System.out.println();
    for (Shift e : problem.getShifts()) {
      System.out.println(e);
    }
    System.out.println();
    for (ShiftGroup e : problem.getShiftGroups()) {
      System.out.println(e);
    }
    System.out.println();
    for (Contract e : problem.getContracts()) {
      System.out.println("Contract(id=" + e.getId() + ", label=" + e.getLabel());
      for (PatternContract p : e.getPatternContracts()) {
        System.out.println("\t" + p);
      }
    }
    System.out.println();
    for (Staff e : problem.getStaffs()) {
      System.out.println(e);
    }
    System.out.println();
    for (Cover e : problem.getCovers()) {
      System.out.println(e);
    }
    System.out.println();
    for (DayOffRequest e : problem.getDayOffRequests()) {
      System.out.println(e);
    }
    System.out.println();
    for (ShiftOffRequest e : problem.getShiftOffRequests()) {
      System.out.println(e);
    }
    System.out.println();
    for (ShiftOnRequest e : problem.getShiftOnRequests()) {
      System.out.println(e);
    }
    System.out.println();
    for (FixedAssignment e : problem.getFixedAssignments()) {
      System.out.println(e);
    }
  }
}
