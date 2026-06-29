'use strict';

const JOBS = [
  {title:'Senior Java Engineer',company:'FinTech Corp',location:'Toronto, ON (Hybrid)',candidates:42,avgScore:76,status:'active'},
  {title:'Cloud Solutions Architect',company:'TechGlobal',location:'Remote',candidates:28,avgScore:71,status:'active'},
  {title:'Microservices Developer',company:'StartupXYZ',location:'Ottawa, ON',candidates:19,avgScore:68,status:'active'},
  {title:'Backend Engineer',company:'DataSystems Inc',location:'Montreal, QC',candidates:55,avgScore:74,status:'active'},
  {title:'Staff Software Engineer',company:'Enterprise Co',location:'Vancouver, BC',candidates:12,avgScore:81,status:'active'},
];

const CANDIDATES = [
  {rank:1,name:'Arjun Patel',title:'Lead Software Engineer',exp:'9 yrs',skillMatch:92,score:91,status:'shortlisted'},
  {rank:2,name:'Mei Lin Zhang',title:'Sr. Java Developer',exp:'7 yrs',skillMatch:88,score:86,status:'shortlisted'},
  {rank:3,name:'David Okafor',title:'Backend Engineer',exp:'6 yrs',skillMatch:84,score:82,status:'shortlisted'},
  {rank:4,name:'Priya Sharma',title:'Full Stack Developer',exp:'5 yrs',skillMatch:79,score:77,status:'reviewing'},
  {rank:5,name:'Lucas Fernandez',title:'Java Developer',exp:'5 yrs',skillMatch:75,score:73,status:'reviewing'},
  {rank:6,name:'Emma Wilson',title:'Software Engineer',exp:'4 yrs',skillMatch:71,score:68,status:'reviewing'},
  {rank:7,name:'James Kim',title:'Backend Developer',exp:'3 yrs',skillMatch:62,score:58,status:'reviewing'},
  {rank:8,name:'Anna Petrov',title:'Junior Developer',exp:'2 yrs',skillMatch:44,score:41,status:'rejected'},
];

const QUESTIONS = [
  {cat:'Technical',diff:'hard',q:'Design a distributed rate limiter that works across multiple instances of your Spring Boot service using Redis. Walk me through the data structures and consistency guarantees.'},
  {cat:'Technical',diff:'hard',q:'You have a Kafka consumer that processes payment events. How would you handle duplicate messages and ensure exactly-once processing semantics?'},
  {cat:'Technical',diff:'medium',q:'Explain the Saga pattern for managing distributed transactions in microservices. When would you use choreography vs orchestration?'},
  {cat:'Technical',diff:'medium',q:'How would you design the database schema and caching strategy for a high-traffic REST API serving 10,000 requests/second?'},
  {cat:'Behavioral',diff:'medium',q:'Tell me about a time you diagnosed a production incident under pressure. How did you identify the root cause and what did you change to prevent recurrence?'},
  {cat:'Behavioral',diff:'easy',q:'Describe a technical disagreement you had with a teammate. How did you resolve it and what did you learn?'},
  {cat:'Situational',diff:'hard',q:'Your service is experiencing 5x normal traffic. Monitoring shows database connection pool exhaustion. Walk me through your immediate response and longer-term remediation.'},
  {cat:'Situational',diff:'medium',q:'A junior engineer submits a PR with working code that has no tests and violates several architectural guidelines. How do you handle the code review?'},
];

const ANALYSIS_RESULTS = [
  {skillMatch:92,expScore:88,eduScore:85,strengths:['Strong Kafka Streams experience matching job requirement','8+ years with Spring Boot ecosystem','Hands-on Kubernetes deployment experience','Proven microservices architecture patterns'],gaps:['No direct Drools rules engine experience','AWS certification not mentioned','Limited frontend (React) exposure'],summary:'Highly qualified candidate with 9 years of Java/Spring Boot expertise and strong distributed systems background. Skill match with job requirements is excellent at 92%. The experience with Kafka and microservices architecture directly aligns with the Senior Java Engineer role. Minor gaps in Drools and AWS certifications are easily bridgeable.'},
  {skillMatch:78,expScore:75,eduScore:90,strengths:['AWS Solutions Architect certification','Strong cloud-native architecture skills','Excellent system design background','Experience with containerization and Kubernetes'],gaps:['Limited Kafka experience (requires deeper event streaming)','No Spring Boot microservices at scale','Missing financial domain knowledge'],summary:'Strong cloud architecture candidate with AWS certification. Education background is excellent. Moderate fit for this specific role due to limited Kafka and microservices experience at scale, though cloud skills are a significant asset. Recommend technical screening to assess adaptability.'},
  {skillMatch:84,expScore:80,eduScore:82,strengths:['Deep Spring Boot microservices experience','Kafka + event-driven architecture','PostgreSQL and Redis optimization skills','Strong CI/CD pipeline knowledge'],gaps:['No experience with distributed tracing (Jaeger/Zipkin)','Limited leadership experience for Senior role','No mention of load testing practices'],summary:'Well-rounded backend engineer with direct microservices experience. Technical skills are a strong match. The candidate may need mentoring on observability tooling and would benefit from leadership opportunities to grow into the Senior role expectations.'},
  {skillMatch:71,expScore:68,eduScore:85,strengths:['Full-stack versatility (React + Java)','Modern Java 17/21 experience','Docker containerization skills'],gaps:['No Kafka or messaging experience','Limited microservices architecture depth','No Kubernetes experience'],summary:'Capable full-stack developer but lacks the distributed systems depth required for this Senior Java Engineer role. Strong frontend skills are less relevant here. Consider for a junior or mid-level backend role instead.'},
];

let currentAnalysis = null;

function badge(cls, label) { return `<span class="badge badge-${cls}">${label||cls}</span>`; }
function scoreClass(s) { return s >= 80 ? 'high' : s >= 60 ? 'med' : 'low'; }
function rankBadgeClass(r) { return r <= 3 ? `rank-${r}` : 'rank-n'; }

function renderJobs() {
  document.getElementById('jobsBody').innerHTML = JOBS.map(j => `<tr>
    <td><strong>${j.title}</strong></td>
    <td>${j.company}</td>
    <td>${j.location}</td>
    <td>${j.candidates}</td>
    <td><span class="score-pill score-${scoreClass(j.avgScore)}">${j.avgScore}%</span></td>
    <td>${badge('active')}</td>
  </tr>`).join('');
}

function renderRankings() {
  document.getElementById('rankingsBody').innerHTML = CANDIDATES.map(c => `<tr>
    <td><div class="rank-badge ${rankBadgeClass(c.rank)}">${c.rank}</div></td>
    <td><strong>${c.name}</strong></td>
    <td>${c.title}</td>
    <td>${c.exp}</td>
    <td><span class="score-pill score-${scoreClass(c.skillMatch)}">${c.skillMatch}%</span></td>
    <td><span class="score-pill score-${scoreClass(c.score)}">${c.score}</span></td>
    <td>${badge(c.status)}</td>
  </tr>`).join('');
}

function renderQuestions() {
  const catColors = {Technical:'badge-shortlisted',Behavioral:'badge-reviewing',Situational:'badge-active'};
  document.getElementById('questionsGrid').innerHTML = QUESTIONS.map((q, i) => `
    <div class="q-card">
      <div class="q-header">
        <div class="q-num">${i+1}</div>
        <span class="badge ${catColors[q.cat]}">${q.cat}</span>
        <span class="badge badge-${q.diff}">${q.diff.toUpperCase()}</span>
        <div class="q-cat" style="margin-left:auto">${q.cat}</div>
      </div>
      <div class="q-text">${q.q}</div>
    </div>`).join('');
}

function analyzeResume() {
  const text = document.getElementById('resumeText').value.trim();
  if (!text) { alert('Please paste a resume to analyze.'); return; }

  const jobId = parseInt(document.getElementById('jobSelect').value) - 1;
  document.getElementById('analysisResult').style.display = 'none';
  document.getElementById('analyzingCard').style.display = 'flex';

  setTimeout(() => {
    document.getElementById('analyzingCard').style.display = 'none';
    showAnalysis(ANALYSIS_RESULTS[jobId] || ANALYSIS_RESULTS[0]);
  }, 2800);
}

function showAnalysis(r) {
  currentAnalysis = r;
  const card = document.getElementById('analysisResult');
  card.style.display = 'block';

  const overall = Math.round((r.skillMatch + r.expScore + r.eduScore) / 3);
  const scoreBadge = document.getElementById('analysisScore');
  scoreBadge.textContent = `${overall}/100`;
  scoreBadge.className = `score-badge score-pill score-${scoreClass(overall)}`;

  setTimeout(() => {
    animatePct('skillBar', 'skillPct', r.skillMatch);
    animatePct('expBar', 'expPct', r.expScore);
    animatePct('eduBar', 'eduPct', r.eduScore);
  }, 100);

  document.getElementById('strengthsList').innerHTML = r.strengths.map(s => `<li>${s}</li>`).join('');
  document.getElementById('gapsList').innerHTML = r.gaps.map(g => `<li>${g}</li>`).join('');
  document.getElementById('aiSummary').textContent = r.summary;
}

function animatePct(barId, pctId, target) {
  document.getElementById(barId).style.width = target + '%';
  let current = 0;
  const step = target / 40;
  const timer = setInterval(() => {
    current = Math.min(current + step, target);
    document.getElementById(pctId).textContent = Math.round(current) + '%';
    if (current >= target) clearInterval(timer);
  }, 25);
}

function showTab(tab, link) {
  document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
  document.querySelectorAll('.nav-link').forEach(l => l.classList.remove('active'));
  document.getElementById('tab-' + tab).classList.add('active');
  if (link) link.classList.add('active');
}

renderJobs();
renderRankings();
renderQuestions();
