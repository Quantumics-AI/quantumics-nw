import { Component, Input, OnInit } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { SnackbarService } from 'src/app/core/services/snackbar.service';
import { FormGroup, FormBuilder, Validators, FormControl } from '@angular/forms';
import {  ActivatedRoute, Router } from '@angular/router';
import { Observable, Subject } from 'rxjs';
import { map, takeUntil } from 'rxjs/operators';
import { Certificate } from 'src/app/models/certificate';
import { Quantumfacade } from 'src/app/state/quantum.facade';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ViewResultComponent } from '../view-result/view-result.component';
import { RuleCreationService } from '../../services/rule-creation.service';
import { ConfirmationComponent } from '../confirmation/confirmation.component';

@Component({
  selector: 'app-view-running-rules',
  templateUrl: './view-running-rules.component.html',
  styleUrls: ['./view-running-rules.component.scss']
})
export class ViewRunningRulesComponent implements OnInit {
  // @Input() projectId: number;
  // @Input() userId: number;

  fg: FormGroup;
  projectId: number;
  userId: number;
  certificate$: Observable<Certificate>;
  certificateData: Certificate;
  private unsubscribe: Subject<void> = new Subject<void>();
  public loading:boolean;
  public runningList: any;
  public cancelBtn: boolean;
  public outputData: any;
  public startIndex: number = 0;
  public pageSize: number = 10;
  public endIndex: number = this.pageSize;
  public page = 1;
  // public dataProfiler = {
  //   "match" : false,
  //   "pass" : false,
  //   "ruleTypeName" : "Data Profiler",
  //   "levelName" : "Column level",
  //   "SourceFile" : "s3a://qsai-nw-src/DuplicateValues/Duplicate_Value_Data_Null.csv",
  //   "TargetFile" : "s3a://qsai-nw-src/DuplicateValues/Duplicate_Value_Data_Null.csv",
  //   "data" : [
  //     {
  //       "dataset" : "Date Type",
  //       "source" : "Number",
  //       "target" : "Number",
  //       "match" : true
  //     },
  //     {
  //       "dataset" : "Column",
  //       "source" : "Salary",
  //       "target" : "Salary",
  //       "match" : true
  //     },
  //     {
  //       "dataset" : "Missing",
  //       "source" : 0,
  //       "target" : 0,
  //       "match" : true
  //     },
  //     {
  //       "dataset" : "Missing %",
  //       "source" : 0.00,
  //       "target" : 0.00,
  //       "match" : true
  //     },
  //     {
  //       "dataset" : "Unique",
  //       "source" : 900,
  //       "target" : 800,
  //       "match" : false
  //     },
  //     {
  //       "dataset" : "Unique %",
  //       "source" : 90.00,
  //       "target" : 80.00,
  //       "match" : false
  //     },
  //     {
  //       "dataset" : "Minimum",
  //       "source" : 100000,
  //       "target" : 100000,
  //       "match" : true
  //     },
  //     {
  //       "dataset" : "Maximum",
  //       "source" : 250000,
  //       "target" : 250000,
  //       "match" : true
  //     },
  //     {
  //       "dataset" : "Median",
  //       "source" : 150000,
  //       "target" : 150000,
  //       "match" : true
  //     },
  //     {
  //       "dataset" : "Mean",
  //       "source" : 125000,
  //       "target" : 125000,
  //       "match" : true
  //     }
  //   ]
    
  //   }

  constructor(
    // public modal: NgbActiveModal,
    private snakbar: SnackbarService,
    private fb: FormBuilder,
    private router: Router,
    private activatedRoute: ActivatedRoute,
    private quantumFacade: Quantumfacade,
    private modalService: NgbModal,
    private ruleCreationService: RuleCreationService,
  ){
    // this.projectId = parseInt(this.activatedRoute.parent.snapshot.paramMap.get('projectId'), 10);
    this.certificate$ = this.quantumFacade.certificate$;
    this.certificate$
      .pipe(takeUntil(this.unsubscribe))
      .subscribe(certificate => {
        this.certificateData = certificate;
        this.userId = +certificate.user_id;
      });
  }

  ngOnInit(): void {
    this.projectId = +localStorage.getItem('project_id');
    this.getRulJobs();
  }

  public getRulJobs(): void {
    // this.loading = true;
    const refreshPage = () => {
        this.autoRefresh();
    };
    this.ruleCreationService.getRuleJobs(this.userId, this.projectId).subscribe((response) => {
      console.log("Rule data:", response);
      this.runningList = response?.result;
      this.loading = false;
      if (this.runningList.length > 1) {
        this.runningList.sort((val1, val2) => {
          return (
            (new Date(val2.createdDate) as any) -
            (new Date(val1.createdDate) as any)
          );
        });
      }
      // Check if there is at least one object with jobStatus "Inprocess"
      const hasInprocessJob = this.runningList.some(item => item.jobStatus === 'Inprocess' || item.jobStatus === 'Not Started');
      // If there is an in-process job, show the button; otherwise, hide it
      if (hasInprocessJob) {
        setTimeout(refreshPage, 5000);
        this.cancelBtn = true;
      } else {
        this.cancelBtn = false;
      }
    }, (error) => {
      this.loading = false;
    });
  }

  public autoRefresh(): void {
    // const autoRefreshInterval = 5000; // 5 seconds

    // const refreshPage = () => {
    //   location.reload();
    // };

    this.getRulJobs();
  }

  public viewResult(r: any): void {
    const data = JSON.parse(r.jobOutput);

    if (Array.isArray(data)) {
      // It's a JSON array, you can loop through its elements
      for (const obj of data) {
        // Process each object in the array
        this.outputData = obj
        // You can add your conditional logic here for JSON array items
      }
    } else {
      // It's a JSON object
      this.outputData = data;
      // You can add your conditional logic for JSON objects here
    }

    const modalRef = this.modalService.open(ViewResultComponent, { size: 'md', windowClass: 'modal-size', scrollable: false });
    modalRef.componentInstance.userId = this.userId;
    modalRef.componentInstance.projectId = this.projectId;
    modalRef.componentInstance.output = this.outputData;
    modalRef.result.then((result) => {
      
    }, (result) => {
      
    });
  }

  public cancelInprocess(): void {
    const modalRef = this.modalService.open(ConfirmationComponent, { size: 'md modal-dialog-centered', scrollable: false});
    modalRef.result.then((result) => {
      this.loading = true;
      // Initialize an empty array to store jobIds
      const jobIds: number[] = [];
  
      // Iterate through the runningList to find objects with jobStatus "Inprocess"
      for (const item of this.runningList) {
          if (item.jobStatus === 'Inprocess' || item.jobStatus === 'Not Started' ) {
              // Push the jobIds into the array
              jobIds.push(item.jobId);
          }
      }
  
      const req = {
        jobIds : jobIds
      };
  
      this.ruleCreationService.cancelRunningRule(this.userId, this.projectId, req).subscribe((res) => {
        this.loading = false;
        this.snakbar.open(res.message);
        this.getRulJobs();
      }, (error) => {
        this.loading = false;
      });
  
      // Now, 'jobIds' contains all the jobIds for objects with jobStatus "Inprocess"
      console.log(jobIds); // You can use the 'jobIds' array as needed
    }, (error) => {

    });
    
  }

  public refresh(): void {
    // call get function
    this.getRulJobs();
  }

  public back(): void {
    this.router.navigate([`projects/${this.projectId}/data-quality`]);
  }

  // Define a function to parse the jobOutput JSON string
  parseJobOutput(jobOutput: string) {
    try {
      const parsedOutput = JSON.parse(jobOutput);
      if (Array.isArray(parsedOutput)) {
        // Check if it's an array and contains an item with 'match' property
        const hasMatch = parsedOutput.some(item => item.match);
        return hasMatch ? 'Match' : 'MisMatch';
      } else if (parsedOutput.match !== undefined) {
        // Check if it's an object with 'match' property
        return parsedOutput.match ? 'Match' : 'MisMatch';
      } else {
        // If it doesn't have 'match' property, default to 'MisMatch'
        return 'MisMatch';
      }
    } catch (error) {
      // Handle the case where JSON parsing fails
      return 'MisMatch';
    }
  }

  public onPageChange(currentPage: number): void {
    this.startIndex = (currentPage - 1) * this.pageSize;
    this.endIndex = this.startIndex + this.pageSize;
  }
}
