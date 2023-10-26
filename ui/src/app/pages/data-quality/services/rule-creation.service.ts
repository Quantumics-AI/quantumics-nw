import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map } from 'rxjs/operators';
import { Quantumfacade } from 'src/app/state/quantum.facade';
// import { request } from 'http';

@Injectable({
  providedIn: 'root'
})
export class RuleCreationService {

  constructor(private http: HttpClient, private quantumfacade: Quantumfacade) { }

  public getDataConnection(projectId: number, userId: number): Observable<any> {
    return this.http.get(`/QuantumSparkServiceAPI/api/v1/aws/getConnections/${projectId}/${userId}`);
  }

  // public  {
  //   return this.http.post(`/QuantumSparkServiceAPI/api/v1/qsrules/${userId}/${projectId}`);
  // }

  public saveRule(userId:number, projectId:number, request: any): Observable<any> {
    const headers = new HttpHeaders().set('Content-Type', 'application/json');
    return this.http.post(`/QuantumSparkServiceAPI/api/v1/qsrules/${userId}/${projectId}`, request, { headers }).pipe(
      map((response: any) => {
        return response;
      })
    );
  }

  public updateRule(userId:number, projectId:number, data: any): Observable<any> {
    return this.http.put(`/QuantumSparkServiceAPI/api/v1/qsrules/${userId}/${projectId}`, data);
  }

  // QuantumSparkServiceAPI/api/v1/qsrules/1?status=Active&page=1&size=10
  public getRulesData(userId:number, projectId: number, status: string, pageNumber: number, sizeLength: number): Observable<any> {
    return this.http.get(`/QuantumSparkServiceAPI/api/v1/qsrules/${userId}/${projectId}?status=${status}&page=${pageNumber}&size=${sizeLength}`);
  }

  public getRuleTypes(projectId: number, sourceOnly: boolean): Observable<any> {
    return this.http.get(`/QuantumSparkServiceAPI/api/v1/ruletypes/${projectId}?sourceOnly=${sourceOnly}`);
  }

  public getBucketList(userId: number, projectId: number): Observable<any> {
    return this.http.get(`/QuantumSparkServiceAPI/api/v1/aws/buckets/${userId}/${projectId}`);
  }
  // http://localhost:8080/QuantumSparkServiceAPI/api/v1/qsrules/{userId}/{projectId}/{ruleId}
  public getEditRule(userId: number, projectId: number, ruleId: number): Observable<any> {
    return this.http.get(`/QuantumSparkServiceAPI/api/v1/qsrules/${userId}/${projectId}/${ruleId}`);
  }

  public getBrowseFile(userId: number, projectId: number, bucketName: string): Observable<any> {
    return this.http.get(`/QuantumSparkServiceAPI/api/v1/aws/buckets/${userId}/${projectId}/${bucketName}`);
  }

  //file content
  public getFileContent(userId: number, projectId: number, bucketName: string, filePath: string): Observable<any> {
    return this.http.get(`/QuantumSparkServiceAPI/api/v1/aws/content/${userId}/${projectId}?bucket=${bucketName}&file=${filePath}`);
  }

  // rule job
  public runRule(userId:number, projectId:number, request: any): Observable<any> {
    const headers = new HttpHeaders().set('Content-Type', 'application/json');
    return this.http.post(`/QuantumSparkServiceAPI/api/v1/rulejob/${userId}/${projectId}`, request, { headers }).pipe(
      map((response: any) => {
        return response;
      })
    );
  }

  public getRuleJobs(userId: number, projectId: number): Observable<any> {
    return this.http.get(`/QuantumSparkServiceAPI/api/v1/rulejob/${userId}/${projectId}`);
  }

  public cancelRunningRule(userId:number, projectId:number, data: any): Observable<any> {
    return this.http.put(`/QuantumSparkServiceAPI/api/v1/rulejob/${userId}/${projectId}`, data);
  }

  public existRuleName(userId:number, projectId:number,rulename: string): Observable<any> {
    return this.http.get(`/QuantumSparkServiceAPI/api/v1/qsrules/getRuleByName/${userId}/${projectId}/${rulename}`);
  }
}
