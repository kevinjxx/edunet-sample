using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Net;
using System.Runtime.Serialization;
using System.IO;
using System.Runtime.Serialization.Json;
using MsgPack;
using MsgPack.Serialization;

namespace EdunetSample
{

    [DataContract]
    public class Credential
    {
        [DataMember(IsRequired = true)]
        public string email { get; set; }
        [DataMember(IsRequired = true)]
        public string password { get; set; }
    }
    [DataContract]
    public class AuthToken
    {
        [DataMember(IsRequired = true)]
        public string auth_token { get; set; }
    }
    [DataContract]
    public class User
    {
        [DataMember(IsRequired = true)]
        public string email { get; set; }
        [DataMember(IsRequired = true)]
        public string name { get; set; }
        [DataMember]
        public string nickname { get; set; }
        [DataMember]
        public List<string> roles { get; set; }
    }
    [DataContract]
    public class Annotation
    {
        [DataMember(IsRequired = true)]
        public string guid { get; set; }
        [DataMember]
        public string course_id { get; set; }
        [DataMember]
        public long created_at { get; set; }
        [DataMember]
        public long server_at { get; set; }
    }
    [DataContract]
    public class AnnotationList
    {
        [DataMember(IsRequired = true)]
        public List<Annotation> annotations;
    }
    [DataContract]
    public class StudyJournal
    {
        [DataMember(IsRequired = true)]
        public string guid { get; set; }
        [DataMember]
        public string course_id { get; set; }
        [DataMember]
        public long start_time { get; set; }
        [DataMember]
        public long end_time { get; set; }
        [DataMember]
        public long server_at { get; set; }
        [DataMember]
        public int seconds { get; set; }
    }
    [DataContract]
    public class StudyJournalList
    {
        [DataMember(IsRequired = true)]
        public List<StudyJournal> journals;
    }
    
    class Program
    {
        private string email = "admin@local.host";
        private string password = "changeme";
        private String apiURL = "http://edunet.herokuapp.com/";
        //private string apiURL = "http://192.168.167.1:3000/";

        private T makeHTTPGETJSONRequest<T>(String url) {
            HttpWebRequest request = (HttpWebRequest)WebRequest.Create(url);
            request.Method = "GET";
            using (HttpWebResponse response = (HttpWebResponse)request.GetResponse())
            {
                return (T)new DataContractJsonSerializer(typeof(T)).ReadObject(response.GetResponseStream());
            }
        }

        private T makeHTTPPOSTJSONRequest<T,V>(String url, V content)
        {
            HttpWebRequest request = (HttpWebRequest)WebRequest.Create(url);
            request.Method = "POST";
            request.ContentType = "application/json";
            Stream stream = request.GetRequestStream();
            new DataContractJsonSerializer(content.GetType()).WriteObject(stream, content);
            stream.Close();

            using (HttpWebResponse response = (HttpWebResponse)request.GetResponse())
            {
                return (T)new DataContractJsonSerializer(typeof(T)).ReadObject(response.GetResponseStream());
            }
        }

        private T makeHTTPGETMsgPackRequest<T>(String url)
        {
            HttpWebRequest request = (HttpWebRequest)WebRequest.Create(url);
            request.Method = "GET";
            using (HttpWebResponse response = (HttpWebResponse)request.GetResponse())
            {
                var serializer = MessagePackSerializer.Create<T>();
                return serializer.Unpack(new BufferedStream(response.GetResponseStream()));
            }
        }

        private T makeHTTPPOSTMsgPackRequest<T, V>(String url, V content)
        {
            HttpWebRequest request = (HttpWebRequest)WebRequest.Create(url);
            request.Method = "POST";
            request.ContentType = "application/m-pack";
            Stream stream = request.GetRequestStream();

            var serializer = MessagePackSerializer.Create<V>();
            serializer.Pack(stream, content);
            stream.Close();

            using (HttpWebResponse response = (HttpWebResponse)request.GetResponse())
            {
                return MessagePackSerializer.Create<T>().Unpack(new BufferedStream(response.GetResponseStream()));
            }

        }

        private void makeHTTPPOSTMsgPackRequest<V>(String url, V content)
        {
            HttpWebRequest request = (HttpWebRequest)WebRequest.Create(url);
            request.Method = "POST";
            request.ContentType = "application/x-mpac";
            Stream stream = request.GetRequestStream();
            var context = new SerializationContext() { SerializationMethod = SerializationMethod.Map };
            var serializer = MessagePackSerializer.Create<V>(context);
            serializer.Pack(stream, content);
            stream.Close();
            using (HttpWebResponse response = (HttpWebResponse)request.GetResponse())
            {
            }
        }

        private string createToken()
        {
            Credential cred = new Credential();
            cred.email = email;
            cred.password = password;
            AuthToken token = makeHTTPPOSTJSONRequest<AuthToken, Credential>(apiURL + "token.json", cred);
            return token.auth_token;
        }

        private List<User> listUsers(String token)
        {
            string url = apiURL + "users.mpac?auth_token=" + token;
            return makeHTTPGETMsgPackRequest<List<User>>(url);
        }

	    private List<StudyJournal> listStudyJournals(String token, long since) {
		    String url = apiURL + "study_journals.mpac?auth_token=" + token + "&since="+since;
		    return makeHTTPGETMsgPackRequest<List<StudyJournal>>(url);
        }

	    private void createStudyJournal(String token) {
            TimeSpan _TimeSpan = (DateTime.UtcNow - new DateTime(1970, 1, 1, 0, 0, 0));
		    long now = (long)_TimeSpan.TotalMilliseconds;

            var list = new StudyJournalList();
            list.journals = new List<StudyJournal>();
		    for  (int i = 0; i < 10; i++) {
                StudyJournal obj = new StudyJournal();
                obj.guid = Guid.NewGuid().ToString();
	    	    obj.course_id = "course0001";    	  
	    	    obj.start_time = now - 60000;    	  
	    	    obj.end_time = now;    	  
	    	    obj.seconds = 60;    	  
	    	    list.journals.Add(obj);
		    }
    	    makeHTTPPOSTMsgPackRequest<StudyJournalList>(apiURL + "study_journals.json?auth_token=" + token, list);
        }

        private List<Annotation> listAnnotations(String token, long since)
        {
            String url = apiURL + "annotations.mpac?auth_token=" + token + "&since=" + since;
            return makeHTTPGETMsgPackRequest<List<Annotation>>(url);
        }

        private void createAnnotation(String token)
        {
            TimeSpan _TimeSpan = (DateTime.UtcNow - new DateTime(1970, 1, 1, 0, 0, 0));
            long now = (long)_TimeSpan.TotalMilliseconds;

            var list = new AnnotationList();
            list.annotations = new List<Annotation>();
            for (int i = 0; i < 10; i++)
            {
                var obj = new Annotation();
                obj.guid = Guid.NewGuid().ToString();
                obj.course_id = "course0001";
                list.annotations.Add(obj);
            }
            makeHTTPPOSTMsgPackRequest<AnnotationList>(apiURL + "annotations.json?auth_token=" + token, list);
        }

        static void Main(string[] args)
        {
            Program app = new Program();
            string token = app.createToken();

            List<User> users = app.listUsers(token);
            Console.WriteLine(users);

            //app.createStudyJournal(token);
            //app.createAnnotation(token);

            bool eof = false;
            long since = 0;
            while(!eof) {
    	        var list = app.listStudyJournals(token, since+1);
    	        foreach(var it in list) {
        	        since = it.server_at;
        	        String guid = it.guid;
                    Console.WriteLine(guid);
                }
    	        eof = list.Count == 0;
            }

            eof = false;
            since = 0;
            while (!eof)
            {
                var list = app.listAnnotations(token, since + 1);
                foreach (var it in list)
                {
                    since = it.server_at;
                    String guid = it.guid;
                    Console.WriteLine(guid);
                }
                eof = list.Count == 0;
            }
        }
    }
}
