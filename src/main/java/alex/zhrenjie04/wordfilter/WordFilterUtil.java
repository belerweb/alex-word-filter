package alex.zhrenjie04.wordfilter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import alex.zhrenjie04.wordfilter.result.FilteredResult;
import alex.zhrenjie04.wordfilter.search.tree.Node;
/**
 * 多叉树关键词过滤
 * @author Alex.Zhangrj
 * 北京师范大学计算机系2000级 张人杰
 * zhrenjie04@126.com
 * alex.zhangrj@hotmail.com
 */
public class WordFilterUtil {

	private static Node tree;

	static {
		tree = new Node();
		InputStream is = WordFilterUtil.class.getResourceAsStream("/words.dict");
		try {
			InputStreamReader reader = new InputStreamReader(is, "UTF-8");
			Properties prop = new Properties();
			prop.load(reader);
			Enumeration<String> en = (Enumeration<String>)prop.propertyNames();
			while(en.hasMoreElements()){
				String word = en.nextElement();
				insertWord(word,Integer.valueOf(prop.getProperty(word)));
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			if(is!=null)
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}

	private static void insertWord(String word,int level){
		Node node = tree;
		for(int i=0;i<word.length();i++){
			node = node.addChar(word.charAt(i));
		}
		node.setEnd(true);
		node.setLevel(level);
	}

	private static boolean isPunctuationChar(String c) {
		String regex = "[\\pP\\pZ\\pS\\pM\\pC]";
		Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(c);
		return m.find();
	}

	private static PunctuationOrHtmlFilteredResult filterPunctation(String originalString){
		StringBuffer filteredString=new StringBuffer();
		ArrayList<Integer> charOffsets=new ArrayList<Integer>();
		for(int i=0;i<originalString.length();i++){
			String c = String.valueOf(originalString.charAt(i));
			if (!isPunctuationChar(c)) {
				filteredString.append(c);
				charOffsets.add(i);
			}
		}
		PunctuationOrHtmlFilteredResult result = new PunctuationOrHtmlFilteredResult();
		result.setOriginalString(originalString);
		result.setFilteredString(filteredString);
		result.setCharOffsets(charOffsets);
		return result;
	}

	private static PunctuationOrHtmlFilteredResult filterPunctationAndHtml(String originalString){
		StringBuffer filteredString=new StringBuffer();
		ArrayList<Integer> charOffsets=new ArrayList<Integer>();
		for(int i=0,k=0;i<originalString.length();i++){
			String c = String.valueOf(originalString.charAt(i));
			if (originalString.charAt(i) == '<') {
				for(k=i+1;k<originalString.length();k++) {
					if (originalString.charAt(k) == '<') {
						k = i;
						break;
					}
					if (originalString.charAt(k) == '>') {
						break;
					}
				}
				i = k;
			} else {
				if (!isPunctuationChar(c)) {
					filteredString.append(c);
					charOffsets.add(i);
				}
			}
		}
		PunctuationOrHtmlFilteredResult result = new PunctuationOrHtmlFilteredResult();
		result.setOriginalString(originalString);
		result.setFilteredString(filteredString);
		result.setCharOffsets(charOffsets);
		return result;
	}

	private static FilteredResult filter(PunctuationOrHtmlFilteredResult pohResult, char replacement){
		StringBuffer sentence =pohResult.getFilteredString();
		ArrayList<Integer> charOffsets = pohResult.getCharOffsets();
		StringBuffer resultString = new StringBuffer(pohResult.getOriginalString());
		StringBuffer badWords = new StringBuffer();
		int level=0;
		Node node = tree;
		int start=0,end=0;
		for(int i=0;i<sentence.length();i++){
			start=i;
			end=i;
			node = tree;
			for(int j=i;j<sentence.length();j++){
				node = node.findChar(sentence.charAt(j));
				if(node == null){
					break;
				}
				if(node.isEnd()){
					end=j;
					level = node.getLevel();
				}
			}
			if(end>start){
				for(int k=start;k<=end;k++){
					resultString.setCharAt(charOffsets.get(k), replacement);
				}
				if(badWords.length()>0)badWords.append(",");
				badWords.append(sentence.substring(start, end+1));
				i=end;
			}
		}
		FilteredResult result = new FilteredResult();
		result.setOriginalContent(pohResult.getOriginalString());
		result.setFilteredContent(resultString.toString());
		result.setBadWords(badWords.toString());
		result.setLevel(level);
		return result;
	}
	
	/**
	 * 简单句子过滤
	 * 不处理特殊符号，不处理html，简单句子的过滤
	 * 不能过滤中间带特殊符号的关键词，如：黄_色_漫_画
	 * @param sentence 需要过滤的句子
	 * @param replacement 关键词替换的字符
	 * @return 过滤后的句子
	 */
	public static String simpleFilter(String sentence, char replacement){
		StringBuffer sb=new StringBuffer();
		Node node = tree;
		int start=0,end=0;
		for(int i=0;i<sentence.length();i++){
			start=i;
			end=i;
			node = tree;
			for(int j=i;j<sentence.length();j++){
				node = node.findChar(sentence.charAt(j));
				if(node == null){
					break;
				}
				if(node.isEnd()){
					end=j;
				}
			}
			if(end>start){
				for(int k=start;k<=end;k++){
					sb.append(replacement);
				}
				i=end;
			}else{
				sb.append(sentence.charAt(i));
			}
		}
		return sb.toString();
	}
	/**
	 * 纯文本过滤，不处理html标签，直接将去除所有特殊符号后的字符串拼接后进行过滤，可能会去除html标签内的文字，比如：如果有关键字“fuckfont”，过滤fuck<font>a</font>后的结果为****<****>a</font>
	 * @param originalString 原始需过滤的串
	 * @param replacement 替换的符号
	 * @return
	 */
	public static FilteredResult filterText(String originalString, char replacement){
		return filter(filterPunctation(originalString), replacement);
	}
	/**
	 * html过滤，处理html标签，不处理html标签内的文字，略有不足，会跳过<>标签内的所有内容，比如：如果有关键字“fuck”，过滤<a title="fuck">fuck</a>后的结果为<a title="fuck">****</a>
	 * @param originalString 原始需过滤的串
	 * @param replacement 替换的符号
	 * @return
	 */
	public static FilteredResult filterHtml(String originalString, char replacement){
		return filter(filterPunctationAndHtml(originalString), replacement);
	}
	public static void main(String[] args){
		System.out.println(WordFilterUtil.simpleFilter("网站黄色漫画网站",'*'));
		FilteredResult result = WordFilterUtil.filterText("网站黄.色,漫,画,网站",'*');
		System.out.println(result.getFilteredContent());
		System.out.println(result.getBadWords());
		System.out.println(result.getLevel());
		result = WordFilterUtil.filterHtml("网站<font>黄</font>.<色<script>,漫,画,网站",'*');
		System.out.println(result.getFilteredContent());
		System.out.println(result.getBadWords());
		System.out.println(result.getLevel());
		
		String str = "我#们#的社 会中  国是我们$多么和谐的一个中###国##人%de民和谐#社#会啊色 ：魔司法";
		// str = "近日，上万名法国民众发起了示威游行，抗议萨科齐政府采取新政策，强行驱逐吉普赛人出境。多个人权组织、反种族主义团体、工会和左翼政党都要求法国停止这项政策。但是法国政府显得一意孤行，他们认为，吉普赛人非法营地是非法交易、教唆儿童行乞、卖淫等犯罪行为的温床，态度坚决地要将吉普赛人遣返。出人意料的是，在法国政府强硬的态度背后，其实是大部分法国民众的支持。在许多欧洲国家，吉普赛人都一样饱受歧视，从未融入过当地社会。为什么吉普赛人一直在城市的边缘流浪。";
		// 5000字
		str = "北京师范大学计算机系2000级 张人杰 开发 alex.zhangrj@hotmail.com zhrenjie04@126.com 黄色网站全国政协十一届四次会议今天开幕。在充满希望的春天里，来自各党派团体、各族各界的全国政协委员汇聚京城，共商国是。" + "我们对大会的召开表示热烈的祝贺！未来五年间，“十二五”蓝图如何铺展挥就？在大有作为的重要战略机遇期，中国怎样更加奋发有为？"
				+ "全国政协十一届一党专制四次会议，承载着亿万人民的殷切嘱托，肩负着承前启后的历史重任。2010年是我国发展历程中很不平凡的一年。面对国际金融危机冲击带来的严重影响和国际国内环境的深刻变化，"
				+ "以胡锦涛同志为总书记的中共中央团结带领全国各族人民，同心同德、攻坚克难，集中力量办好大事喜事，妥善处置急事难事，党和国家各项事业取得了新的显著成就。"
				+ "在全国人民的共同努力下，“十一五”规划确定的目标任务胜利完成，国家面貌发生新的历史性变化，为全面建成小康社会奠定了重要基础。在过去的一年里，人民政协高举爱国主义、社会主义旗帜，把握团结和民主两大主题，"
				+ "发挥协调关系、汇聚力量、建言献策、服务大局重要作用，谱写了人民政协事业发展的新篇章。一年来，人民政协加强思想理论建设，自觉坚持中国共产党的领导，坚定不移地走中国特色社会主义政治发展道路；"
				+ "深入开展协商议政，为编制“十二五”规划和保持经济平稳较快发展献计出力；围绕教育、卫生、安居、食品安全等民生问题，积极推动社会管理创新，帮助党和政府排忧解难；"
				+ "认真贯彻民族宗教政策，促进民族团结与宗教和睦；加强与港澳台侨同胞的团结联谊，为中华民族伟大复兴凝心聚力；推动公共外交、拓展对外交流，为我国发展营造良好外部环境；"
				+ "大力推进经常性工作创新，提高人民政协工作科学化水平。总结一年来的生动实践，我们深切地体会到，推进人民政协事业持续发展，必须牢固树立“五个意识”：牢固树立政治意识，在纷繁复杂形势下保持清醒头脑，"
				+ "在大是大非面前站稳坚定立场，在社会深刻变革中坚持正确方向；牢固树立大局意识，始终服从服务党和国家中心工作；牢固树立群众意识，情牵人民、心系群众，协助党和政府妥善处理各方面利益关系；"
				+ "牢固树立履职意识，把履行政治协商、民主监督、参政议政职能作为重大历史使命；牢固树立委员意识，支持帮助广大委员深入实际、走向基层、贴近群众，在报效国家、服务人民的实践中施展才华、建功立业。"
				+ "2011年是“十二五”开局之年，中国的发展进入全面建设小康社会的关键时期。面对世情、国情的深刻变化，面对难得的历史机遇和诸多风险挑战，我们要倍加珍惜来之不易的良好发展局面，"
				+ "深入贯彻中共十七届五中全会和中央经济工作会议精神，继续抓住和用好重要战略机遇期，紧紧围绕党和国家中心工作，同心协力搞好政治协商，积极稳妥推进民主监督，扎实有效开展参政议政，"
				+ "汇聚起促进科学发展的强大合力，加快转变经济发展方式，加强和创新社会管理，做好群众工作，促进社会和谐稳定，为实现“十二五”良好开局、夺取全面建设小康社会新胜利作出贡献。成就鼓舞人心，"
				+ "蓝图催人奋进。让我们更加紧密地团结在以胡锦涛同志为总书记的中共中央周围，以邓小平理论和“三个代表”重要思想为指导，深入贯彻落实科学发展观，再接再厉、锐意进取，为13亿人民谋取更多福祉，为中华民族赢得更大荣光。预祝大会圆满成功。"
				+ "我们对大会的召开表示热烈的祝贺！未来五年间，“十二五”蓝图如何铺展挥就？在大有作为的重要战略机遇期，中国怎样更加奋发有为？" + "全国政协十一届四次会议，承载着亿万人民的殷切嘱托，肩负着承前启后的历史重任。2010年是我国发展历程中很不平凡的一年。面对国际金融危机冲击带来的严重影响和国际国内环境的深刻变化，"
				+ "以胡锦涛同志为总书记的中共中央团结带领全国各族人民，同心同德、攻坚克难，集中力量办好大事喜事，妥善处置急事难事，党和国家各项事业取得了新的显著成就。"
				+ "在全国人民的共同努力下，“十一五”规划确定的目标任务胜利完成，国家面貌发生新的历史性变化，为全面建成小康社会奠定了重要基础。在过去的一年里，人民政协高举爱国主义、社会主义旗帜，把握团结和民主两大主题，"
				+ "发挥协调关系、汇聚力量、建言献策、服务大局重要作用，谱写了人民政协事业发展的新篇章。一年来，人民政协加强思想理论建设，自觉坚持中国共产党的领导，坚定不移地走中国特色社会主义政治发展道路；"
				+ "深入开展协商议政，为编制“十二五”规划和保持经济平稳较快发展献计出力；围绕教育、卫生、安居、食品安全等民生问题，积极推动社会管理创新，帮助党和政府排忧解难；"
				+ "认真贯彻民族宗教政策，促进民族团结与宗教和睦；加强与港澳台侨同胞的团结联谊，为中华民族伟大复兴凝心聚力；推动公共外交、拓展对外交流，为我国发展营造良好外部环境；"
				+ "大力推进经常性工作创新，提高人民政协工作科学化水平。总结一年来的生动实践，我们深切地体会到，推进人民政协事业持续发展，必须牢固树立“五个意识”：牢固树立政治意识，在纷繁复杂形势下保持清醒头脑，"
				+ "在大是大非面前站稳坚定立场，在社会深刻变革中坚持正确方向；牢固树立大局意识，始终服从服务党和国家中心工作；牢固树立群众意识，情牵人民、心系群众，协助党和政府妥善处理各方面利益关系；"
				+ "牢固树立履职意识，把履行政治协商、民主监督、参政议政职能作为重大历史使命；牢固树立委员意识，支持帮助广大委员深入实际、走向基层、贴近群众，在报效国家、服务人民的实践中施展才华、建功立业。"
				+ "2011年是“十二五”开局之年，中国的发展进入全面建设小康社会的关键时期。面对世情、国情的深刻变化，面对难得的历史机遇和诸多风险挑战，我们要倍加珍惜来之不易的良好发展局面，"
				+ "深入贯彻中共十七届五中全会和中央经济工作会议精神，继续抓住和用好重要战略机遇期，紧紧围绕党和国家中心工作，同心协力搞好政治协商，积极稳妥推进民主监督，扎实有效开展参政议政，"
				+ "汇聚起促进科学发展的强大合力，加快转变经济发展方式，加强和创新社会管理，做好群众工作，促进社会和谐稳定，为实现“十二五”良好开局、夺取全面建设小康社会新胜利作出贡献。成就鼓舞人心，"
				+ "蓝图催人奋进。让我们更加紧密地团结在以胡锦涛同志为总书记的中共中央周围，以邓小平理论和“三个代表”重要思想为指导，深入贯彻落实科学发展观，再接再厉、锐意进取，为13亿人民谋取更多福祉，为中华民族赢得更大荣光。预祝大会圆满成功。"
				+ "我们对大会的召开表示热烈的祝贺！未来五年间，“十二五”蓝图如何铺展挥就？在大有作为的重要战略机遇期，中国怎样更加奋发有为？" + "全国政协十一届四次会议，承载着亿万人民的殷切嘱托，肩负着承前启后的历史重任。2010年是我国发展历程中很不平凡的一年。面对国际金融危机冲击带来的严重影响和国际国内环境的深刻变化，"
				+ "以胡锦涛同志为总书记的中共中央团结带领全国各族人民，同心同德、攻坚克难，集中力量办好大事喜事，妥善处置急事难事，党和国家各项事业取得了新的显著成就。"
				+ "在全国人民的共同努力下，“十一五”规划确定的目标任务胜利完成，国家面貌发生新的历史性变化，为全面建成小康社会奠定了重要基础。在过去的一年里，人民政协高举爱国主义、社会主义旗帜，把握团结和民主两大主题，"
				+ "发挥协调关系、汇聚力量、建言献策、服务大局重要作用，谱写了人民政协事业发展的新篇章。一年来，人民政协加强思想理论建设，自觉坚持中国共产党的领导，坚定不移地走中国特色社会主义政治发展道路；"
				+ "深入开展协商议政，为编制“十二五”规划和保持经济平稳较快发展献计出力；围绕教育、卫生、安居、食品安全等民生问题，积极推动社会管理创新，帮助党和政府排忧解难；"
				+ "认真贯彻民族宗教政策，促进民族团结与宗教和睦；加强与港澳台侨同胞的团结联谊，为中华民族伟大复兴凝心聚力；推动公共外交、拓展对外交流，为我国发展营造良好外部环境；"
				+ "大力推进经常性工作创新，提高人民政协工作科学化水平。总结一年来的生动实践，我们深切地体会到，推进人民政协事业持续发展，必须牢固树立“五个意识”：牢固树立政治意识，在纷繁复杂形势下保持清醒头脑，"
				+ "在大是大非面前站稳坚定立场，在社会深刻变革中坚持正确方向；牢固树立大局意识，始终服从服务党和国家中心工作；牢固树立群众意识，情牵人民、心系群众，协助党和政府妥善处理各方面利益关系；"
				+ "牢固树立履职意识，把履行政治协商、民主监督、参政议政职能作为重大历史使命；牢固树立委员意识，支持帮助广大委员深入实际、走向基层、贴近群众，在报效国家、服务人民的实践中施展才华、建功立业。"
				+ "2011年是“十二五”开局之年，中国的发展进入全面建设小康社会的关键时期。面对世情、国情的深刻变化，面对难得的历史机遇和诸多风险挑战，我们要倍加珍惜来之不易的良好发展局面，"
				+ "深入贯彻中共十七届五中全会和中央经济工作会议精神，继续抓住和用好重要战略机遇期，紧紧围绕党和国家中心工作，同心协力搞好政治协商，积极稳妥推进民主监督，扎实有效开展参政议政，"
				+ "汇聚起促进科学发展的强大合力，加快转变经济发展方式，加强和创新社会管理，做好群众工作，促进社会和谐稳定，为实现“十二五”良好开局、夺取全面建设小康社会新胜利作出贡献。成就鼓舞人心，"
				+ "蓝图催人奋进。让我们更加紧密地团结在以胡锦涛同志为总书记的中共中央周围，以邓小平理论和“三个代表”重要思想为指导，深入贯彻落实科学发展观，再接再厉、锐意进取，为13亿人民谋取更多福祉，为中华民族赢得更大荣光。预祝大会圆满成功。"
				+ "我们对大会的召开表示热烈的祝贺！未来五年间，“十二五”蓝图如何铺展挥就？在大有作为的重要战略机遇期，中国怎样更加奋发有为？" + "全国政协十一届四次会议，承载着亿万人民的殷切嘱托，肩负着承前启后的历史重任。2010年是我国发展历程中很不平凡的一年。面对国际金融危机冲击带来的严重影响和国际国内环境的深刻变化，"
				+ "以胡锦涛同志为总书记的中共中央团结带领全国各族人民，同心同德、攻坚克难，集中力量办好大事喜事，妥善处置急事难事，党和国家各项事业取得了新的显著成就。"
				+ "在全国人民的共同努力下，“十一五”规划确定的目标任务胜利完成，国家面貌发生新的历史性变化，为全面建成小康社会奠定了重要基础。在过去的一年里，人民政协高举爱国主义、社会主义旗帜，把握团结和民主两大主题，"
				+ "发挥协调关系、汇聚力量、建言献策、服务大局重要作用，谱写了人民政协事业发展的新篇章。一年来，人民政协加强思想理论建设，自觉坚持中国共产党的领导，坚定不移地走中国特色社会主义政治发展道路；"
				+ "深入开展协商议政，为编制“十二五”规划和保持经济平稳较快发展献计出力；围绕教育、卫生、安居、食品安全等民生问题，积极推动社会管理创新，帮助党和政府排忧解难；"
				+ "认真贯彻民族宗教政策，促进民族团结与宗教和睦；加强与港澳台侨同胞的团结联谊，为中华民族伟大复兴凝心聚力；推动公共外交、拓展对外交流，为我国发展营造良好外部环境；"
				+ "大力推进经常性工作创新，提高人民政协工作科学化水平。总结一年来的生动实践，我们深切地体会到，推进人民政协事业持续发展，必须牢固树立“五个意识”：牢固树立政治意识，在纷繁复杂形势下保持清醒头脑，"
				+ "在大是大非面前站稳坚定立场，在社会深刻变革中坚持正确方向；牢固树立大局意识，始终服从服务党和国家中心工作；牢固树立群众意识，情牵人民、心系群众，协助党和政府妥善处理各方面利益关系；"
				+ "牢固树立履职意识，把履行政治协商、民主监督、参政议政职能作为重大历史使命；牢固树立委员意识，支持帮助广大委员深入实际、走向基层、贴近群众，在报效国家、服务人民的实践中施展才华、建功立业。"
				+ "2011年是“十二五”开局之年，中国的发展进入全面建设小康社会的关键时期。面对世情、国情的深刻变化，面对难得的历史机遇和诸多风险挑战，我们要倍加珍惜来之不易的良好发展局面，"
				+ "深入贯彻中共十七届五中全会和中央经济工作会议精神，继续抓住和用好重要战略机遇期，紧紧围绕党和国家中心工作，同心协力搞好政治协商，积极稳妥推进民主监督，扎实有效开展参政议政，"
				+ "汇聚起促进科学发展的强大合力，加快转变经济发展方式，加强和创新社会管理，做好群众工作，促进社会和谐稳定，为实现“十二五”良好开局、夺取全面建设小康社会新胜利作出贡献。成就鼓舞人心，"
				+ "蓝图催人奋进。让我们更加紧密地团结在以胡锦涛同志为总书记的中共中央周围，以邓小平理论和“三个代表”重要思想为指导，深入贯彻落实科学发展观，再接再厉、锐意进取，为13亿人民谋取更多福祉，为中华民族赢得更大荣光。预祝大会圆满成功。黄色小说";

//		str = "黄色小说<script>function aaa(){alert('胡锦涛一党专制');}</script><p><a class=\"vico\" href=\"http://news.qq.com/a/20100910/000341.htm\"><font color=\"#1f376d\">胡</font>锦<font color=\"#1f376d\">涛考察人民大学 向教师祝贺节日，89风波购买了二十四端口交换机送给老师！GCD，黄瓜葫芦！</font></a></p>";
		long start = System.currentTimeMillis();
		result = WordFilterUtil.filterHtml(str,'*');
		long end = System.currentTimeMillis();
		System.out.println("====Time====" + (end - start));
		System.out.println("original:"+result.getOriginalContent());
		System.out.println("result:"+result.getFilteredContent());
		System.out.println("badWords:"+result.getBadWords());
		System.out.println("level:"+result.getLevel());
		start = System.currentTimeMillis();
		result = WordFilterUtil.filterText(str,'*');
		end = System.currentTimeMillis();
		System.out.println("====Time====" + (end - start));
		System.out.println("original:"+result.getOriginalContent());
		System.out.println("result:"+result.getFilteredContent());
		System.out.println("badWords:"+result.getBadWords());
		System.out.println("level:"+result.getLevel());

	}





	private static class PunctuationOrHtmlFilteredResult {
		private String originalString;
		private StringBuffer filteredString;
		private ArrayList<Integer> charOffsets;
		
		public String getOriginalString() {
			return originalString;
		}
		public void setOriginalString(String originalString) {
			this.originalString = originalString;
		}
		public StringBuffer getFilteredString() {
			return filteredString;
		}
		public void setFilteredString(StringBuffer filteredString) {
			this.filteredString = filteredString;
		}
		public ArrayList<Integer> getCharOffsets() {
			return charOffsets;
		}
		public void setCharOffsets(ArrayList<Integer> charOffsets) {
			this.charOffsets = charOffsets;
		}
	}

}
