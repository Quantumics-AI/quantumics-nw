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
      id: 1,
      ruleName: 'DQ Name1',
      status: 'Completed',
      match: 'Match'
    },
    {
      id: 2,
      ruleName: 'DQ Name2',
      status: 'Completed',
      match: 'Match'
    },
    {
      id: 3,
      ruleName: 'DQ Name3',
      status: 'Completed',
      match: 'Mismatch'
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
  }

  public viewResult(): void {
    const modalRef = this.modalService.open(ViewResultComponent, { size: 'md', windowClass: 'modal-size', scrollable: false });
    modalRef.componentInstance.userId = this.userId;
    modalRef.componentInstance.projectId = this.projectId;


    modalRef.result.then((result) => {
      
    }, (result) => {
      
    });
  }

  public back(): void {
    this.router.navigate([`projects/${this.projectId}/data-quality`]);
  }
}
