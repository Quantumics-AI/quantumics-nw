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

  public runningList = [
    {
      jobId: 1,
      ruleId: 1,
      ruleName: "Rule 1",
      jobStatus: "Completed",
      jobOutput: "{\"source\":100,\"target\":100,\"match\":true}",
      userId: 1,
      createdDate: 1696502540587,
      createdBy: "Test Test",
      modifiedBy: "Test Test",
      modifiedDate: 1696502540587
    },
    {
      jobId: 2,
      ruleId: 2,
      ruleName: "Rule 2",
      jobStatus: "InProcess",
      jobOutput: null,
      userId: 1,
      createdDate: 1696502540587,
      createdBy: "Test Test",
      modifiedBy: "Test Test",
      modifiedDate: 1696502540587
    }
  ];

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
    this.ruleCreationService.getRuleJobs(this.userId, this.projectId).subscribe((response) => {
      console.log("Rule data:", response);
      
    })
  }

  public viewResult(r: any): void {
    const data = JSON.parse(r.jobOutput);
    console.log(data);
    const modalRef = this.modalService.open(ViewResultComponent, { size: 'sm', windowClass: 'modal-size', scrollable: false });
    modalRef.componentInstance.userId = this.userId;
    modalRef.componentInstance.projectId = this.projectId;
    modalRef.componentInstance.output = data;


    modalRef.result.then((result) => {
      
    }, (result) => {
      
    });
  }

  public refresh(): void {
    // call get function
  }

  public back(): void {
    this.router.navigate([`projects/${this.projectId}/data-quality`]);
  }

  parseJobOutput(jobOutput: string | null): { match: boolean } | null {
    try {
        return jobOutput ? JSON.parse(jobOutput) : null;
    } catch (e) {
        console.error('Error parsing jobOutput:', e);
        return null;
    }
  }
}
